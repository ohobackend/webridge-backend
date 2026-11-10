-- Flyway V3__heading_compliance_rate.sql
-- Ensure compliance_rate column exists and is nullable on heading_results

ALTER TABLE IF EXISTS heading_results ADD COLUMN IF NOT EXISTS compliance_rate VARCHAR(50) DEFAULT '100%';
ALTER TABLE IF EXISTS heading_results ALTER COLUMN compliance_rate DROP NOT NULL;
