-- pause_control_results, table_results, contrast_results 테이블에 DOM 캡처 이미지 URL 필드 추가
ALTER TABLE pause_control_results ADD COLUMN IF NOT EXISTS screenshot_url TEXT;
ALTER TABLE pause_control_results ADD COLUMN IF NOT EXISTS capture_url TEXT;

ALTER TABLE table_results ADD COLUMN IF NOT EXISTS screenshot_url TEXT;
ALTER TABLE table_results ADD COLUMN IF NOT EXISTS capture_url TEXT;

ALTER TABLE contrast_results ADD COLUMN IF NOT EXISTS screenshot_url TEXT;
ALTER TABLE contrast_results ADD COLUMN IF NOT EXISTS capture_url TEXT;
