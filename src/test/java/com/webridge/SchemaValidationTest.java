package com.webridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("schematest")
@TestPropertySource(locations = "classpath:application-schematest.yml")
class SchemaValidationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Flyway V1 마이그레이션 적용 후 Hibernate ddl-auto: validate 검증이 성공해야 한다")
    void validateSchemaAndEntities() throws Exception {
        assertThat(dataSource).isNotNull();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> tables = new HashSet<>();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }

            // Verify all 23 entity tables exist in DB
            String[] expectedTables = {
                "users", "terms", "social_accounts", "email_verifications",
                "feedbacks", "accessibility_scans", "alt_text_results",
                "alt_text_feedbacks", "contrast_results", "keyboard_results",
                "label_results", "table_results", "video_caption_results",
                "video_autoplay_results", "basic_language_results",
                "markup_error_results", "heading_results", "response_time_results",
                "pause_control_results", "flashing_results", "scan_reports",
                "surveys", "survey_triggers"
            };

            for (String expected : expectedTables) {
                assertThat(tables).as("Table %s must exist in DB", expected).contains(expected);
            }
        }
    }
}
