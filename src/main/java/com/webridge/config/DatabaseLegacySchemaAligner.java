package com.webridge.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseLegacySchemaAligner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean aligned = false;

    @PostConstruct
    public void init() {
        align();
    }

    @Override
    public void run(ApplicationArguments args) {
        align();
    }

    public synchronized void align() {
        if (aligned) {
            return;
        }

        try {
            log.info("Checking and relaxing legacy NOT NULL constraints on scan result tables...");

            String sql = """
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE LOWER(table_schema) = LOWER(current_schema())
                  AND LOWER(table_name) IN (
                    'heading_results', 'response_time_results', 'pause_control_results',
                    'flashing_results', 'markup_error_results', 'basic_language_results',
                    'video_autoplay_results', 'video_caption_results', 'table_results',
                    'label_results', 'keyboard_results', 'contrast_results', 'alt_text_results'
                  )
                  AND is_nullable = 'NO'
                  AND LOWER(column_name) NOT IN ('id', 'scan_id')
            """;

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> col : columns) {
                String tableName = (String) col.get("table_name");
                String colName = (String) col.get("column_name");
                String dataType = (String) col.get("data_type");

                if (tableName == null || colName == null) {
                    continue;
                }

                try {
                    jdbcTemplate.execute(String.format("ALTER TABLE %s ALTER COLUMN %s DROP NOT NULL", tableName, colName));

                    if (dataType != null) {
                        String lowerType = dataType.toLowerCase();
                        if (lowerType.contains("int") || lowerType.contains("numeric") || lowerType.contains("float") || lowerType.contains("double") || lowerType.contains("real")) {
                            jdbcTemplate.execute(String.format("ALTER TABLE %s ALTER COLUMN %s SET DEFAULT 0", tableName, colName));
                        } else if (lowerType.contains("char") || lowerType.contains("text")) {
                            jdbcTemplate.execute(String.format("ALTER TABLE %s ALTER COLUMN %s SET DEFAULT ''", tableName, colName));
                        } else if (lowerType.contains("bool")) {
                            jdbcTemplate.execute(String.format("ALTER TABLE %s ALTER COLUMN %s SET DEFAULT false", tableName, colName));
                        }
                    }
                    log.info("Relaxed legacy column {}.{} (DROP NOT NULL + DEFAULT)", tableName, colName);
                } catch (Exception e) {
                    log.debug("Could not alter column {}.{}: {}", tableName, colName, e.getMessage());
                }
            }
            aligned = true;
            log.info("Scan result tables legacy schema alignment completed.");
        } catch (Exception e) {
            log.warn("DatabaseLegacySchemaAligner execution warning: {}", e.getMessage());
        }
    }
}
