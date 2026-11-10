-- Flyway V1__init.sql
-- WEBridge PostgreSQL Schema (compatible with Django models)

CREATE TABLE IF NOT EXISTS django_content_type (
    id SERIAL PRIMARY KEY,
    app_label VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    CONSTRAINT django_content_type_app_label_model_uniq UNIQUE (app_label, model)
);

CREATE TABLE IF NOT EXISTS auth_permission (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content_type_id INTEGER NOT NULL REFERENCES django_content_type(id) ON DELETE CASCADE,
    codename VARCHAR(100) NOT NULL,
    CONSTRAINT auth_permission_content_type_id_codename_uniq UNIQUE (content_type_id, codename)
);

CREATE TABLE IF NOT EXISTS auth_group (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS auth_group_permissions (
    id BIGSERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES auth_group(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES auth_permission(id) ON DELETE CASCADE,
    CONSTRAINT auth_group_permissions_group_id_permission_id_uniq UNIQUE (group_id, permission_id)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    password VARCHAR(128) NOT NULL,
    last_login TIMESTAMP WITH TIME ZONE NULL,
    is_superuser BOOLEAN NOT NULL DEFAULT FALSE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'email',
    provider_id VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    service_terms_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    age_verification_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_policy_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    service_terms_version VARCHAR(10) NOT NULL DEFAULT '1.0',
    privacy_policy_version VARCHAR(10) NOT NULL DEFAULT '1.0',
    marketing_version VARCHAR(10) NOT NULL DEFAULT '1.0',
    service_terms_agreed_at TIMESTAMP WITH TIME ZONE NULL,
    age_verification_agreed_at TIMESTAMP WITH TIME ZONE NULL,
    privacy_policy_agreed_at TIMESTAMP WITH TIME ZONE NULL,
    marketing_agreed_at TIMESTAMP WITH TIME ZONE NULL,
    is_social_signup_complete BOOLEAN NOT NULL DEFAULT FALSE,
    path_to_webridge VARCHAR(1) NULL,
    path_to_webridge_other VARCHAR(100) NULL,
    occupation VARCHAR(1) NULL,
    occupation_other VARCHAR(100) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS core_user_groups (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id INTEGER NOT NULL REFERENCES auth_group(id) ON DELETE CASCADE,
    CONSTRAINT core_user_groups_user_id_group_id_uniq UNIQUE (user_id, group_id)
);

CREATE TABLE IF NOT EXISTS core_user_user_permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES auth_permission(id) ON DELETE CASCADE,
    CONSTRAINT core_user_user_permissions_user_id_permission_id_uniq UNIQUE (user_id, permission_id)
);

CREATE TABLE IF NOT EXISTS terms (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL UNIQUE,
    version VARCHAR(10) NOT NULL,
    change_type VARCHAR(10) NOT NULL DEFAULT 'major',
    requires_reagreement BOOLEAN NOT NULL DEFAULT TRUE,
    change_summary VARCHAR(200) NOT NULL DEFAULT '',
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS social_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    extra_data JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT social_accounts_provider_provider_id_uniq UNIQUE (provider, provider_id)
);

CREATE TABLE IF NOT EXISTS email_verifications (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    verification_token VARCHAR(255) NOT NULL UNIQUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    rating INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS accessibility_scans (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    progress INTEGER NOT NULL DEFAULT 0,
    total_issues INTEGER NOT NULL DEFAULT 0,
    compliance_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    alt_text_completed BOOLEAN NOT NULL DEFAULT FALSE,
    contrast_completed BOOLEAN NOT NULL DEFAULT FALSE,
    keyboard_completed BOOLEAN NOT NULL DEFAULT FALSE,
    label_completed BOOLEAN NOT NULL DEFAULT FALSE,
    table_completed BOOLEAN NOT NULL DEFAULT FALSE,
    video_caption_completed BOOLEAN NOT NULL DEFAULT FALSE,
    video_autoplay_completed BOOLEAN NOT NULL DEFAULT FALSE,
    basic_language_completed BOOLEAN NOT NULL DEFAULT FALSE,
    markup_error_completed BOOLEAN NOT NULL DEFAULT FALSE,
    heading_completed BOOLEAN NOT NULL DEFAULT FALSE,
    response_time_completed BOOLEAN NOT NULL DEFAULT FALSE,
    pause_control_completed BOOLEAN NOT NULL DEFAULT FALSE,
    flashing_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    error_message TEXT NULL
);

CREATE TABLE IF NOT EXISTS alt_text_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    img_url TEXT NOT NULL,
    element_html TEXT NOT NULL DEFAULT '',
    alt_text TEXT NULL,
    suggested_alt TEXT NOT NULL DEFAULT '',
    compliance INTEGER NOT NULL,
    ai_type VARCHAR(50) NOT NULL DEFAULT '',
    ai_grade VARCHAR(20) NOT NULL DEFAULT '',
    ai_reason TEXT NOT NULL DEFAULT '',
    ai_improvement TEXT NOT NULL DEFAULT '',
    has_error BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS alt_text_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    alt_text_result_id BIGINT NOT NULL REFERENCES alt_text_results(id) ON DELETE CASCADE,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    rating VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS contrast_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    text_content TEXT NOT NULL,
    element_html TEXT NOT NULL DEFAULT '',
    foreground_color VARCHAR(50) NOT NULL,
    background_color VARCHAR(50) NOT NULL,
    contrast_ratio DOUBLE PRECISION NOT NULL,
    wcag_compliant BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS keyboard_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    element_type VARCHAR(50) NOT NULL,
    element_html TEXT NOT NULL DEFAULT '',
    accessible BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS label_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    input_type VARCHAR(50) NOT NULL,
    element_html TEXT NOT NULL DEFAULT '',
    label_present BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS table_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    headers_count INTEGER NOT NULL,
    rows_count INTEGER NOT NULL,
    element_html TEXT NOT NULL DEFAULT '',
    compliant BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS video_caption_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    video_url TEXT NOT NULL DEFAULT '',
    element_html TEXT NOT NULL DEFAULT '',
    has_thumbnail BOOLEAN NOT NULL DEFAULT FALSE,
    has_transcript BOOLEAN NOT NULL DEFAULT FALSE,
    has_audio_description BOOLEAN NOT NULL DEFAULT FALSE,
    has_aria_label BOOLEAN NOT NULL DEFAULT FALSE,
    compliant BOOLEAN NOT NULL DEFAULT FALSE,
    message VARCHAR(200) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS video_autoplay_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    video_url TEXT NOT NULL DEFAULT '',
    element_html TEXT NOT NULL DEFAULT '',
    autoplay_disabled BOOLEAN NOT NULL DEFAULT TRUE,
    keyboard_accessible BOOLEAN NOT NULL DEFAULT FALSE,
    compliant BOOLEAN NOT NULL DEFAULT TRUE,
    message VARCHAR(200) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS basic_language_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    lang_attribute VARCHAR(10) NOT NULL DEFAULT '',
    compliant BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS markup_error_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    error_type VARCHAR(100) NULL,
    error_detail VARCHAR(500) NULL,
    element_id VARCHAR(200) NULL,
    element_html TEXT NULL,
    compliant BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS heading_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    page_title_exists BOOLEAN NOT NULL DEFAULT FALSE,
    page_title_empty BOOLEAN NOT NULL DEFAULT TRUE,
    page_title_text VARCHAR(500) NULL,
    page_title_length INTEGER NOT NULL DEFAULT 0,
    total_popups INTEGER NOT NULL DEFAULT 0,
    popups_without_title INTEGER NOT NULL DEFAULT 0,
    total_frames INTEGER NOT NULL DEFAULT 0,
    frames_without_title INTEGER NOT NULL DEFAULT 0,
    total_headings INTEGER NOT NULL DEFAULT 0,
    empty_headings INTEGER NOT NULL DEFAULT 0,
    headings_without_content INTEGER NOT NULL DEFAULT 0,
    total_semantic_sections INTEGER NOT NULL DEFAULT 0,
    sections_without_headings INTEGER NOT NULL DEFAULT 0,
    special_symbol_issues INTEGER NOT NULL DEFAULT 0,
    total_issues INTEGER NOT NULL DEFAULT 0,
    issues_details JSONB NOT NULL DEFAULT '[]',
    element_html TEXT NOT NULL DEFAULT '',
    compliant BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    compliance_rate VARCHAR(50) NULL DEFAULT '100%',
    detailed_results JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS response_time_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    has_time_limit BOOLEAN NOT NULL DEFAULT FALSE,
    short_timeouts INTEGER NOT NULL DEFAULT 0,
    element_html TEXT NOT NULL DEFAULT '',
    compliant BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pause_control_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    element_html TEXT NOT NULL DEFAULT '',
    compliant BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    has_autoplay BOOLEAN NOT NULL DEFAULT FALSE,
    has_pause BOOLEAN NOT NULL DEFAULT FALSE,
    tag TEXT NULL
);

CREATE TABLE IF NOT EXISTS flashing_results (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    flashing_type VARCHAR(100) NULL,
    element_html TEXT NULL,
    duration INTEGER NULL,
    compliant BOOLEAN NOT NULL,
    message VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS scan_reports (
    id BIGSERIAL PRIMARY KEY,
    scan_id UUID NOT NULL UNIQUE REFERENCES accessibility_scans(id) ON DELETE CASCADE,
    format VARCHAR(10) NOT NULL DEFAULT 'pdf',
    file_path VARCHAR(100) NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    download_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS surveys (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(254) NOT NULL,
    privacy_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    company_type JSONB NOT NULL,
    company_type_other VARCHAR(500) NOT NULL DEFAULT '',
    usage_reason JSONB NOT NULL,
    usage_reason_other VARCHAR(500) NOT NULL DEFAULT '',
    satisfaction_overall INTEGER NOT NULL,
    satisfaction_accuracy INTEGER NOT NULL,
    satisfaction_reuse INTEGER NOT NULL,
    satisfaction_recommend INTEGER NOT NULL,
    user_type VARCHAR(1) NOT NULL,
    user_type_other VARCHAR(500) NOT NULL DEFAULT '',
    purchase_method VARCHAR(1) NOT NULL,
    purchase_method_other VARCHAR(500) NOT NULL DEFAULT '',
    usage_method JSONB NOT NULL,
    usage_method_other VARCHAR(500) NOT NULL DEFAULT '',
    future_feature JSONB NOT NULL,
    future_feature_other VARCHAR(500) NOT NULL DEFAULT '',
    improvement_opinion TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS survey_triggers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    completed_scans_count INTEGER NOT NULL DEFAULT 0,
    has_issues_found BOOLEAN NOT NULL DEFAULT FALSE,
    survey_shown BOOLEAN NOT NULL DEFAULT FALSE,
    survey_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS django_admin_log (
    id SERIAL PRIMARY KEY,
    action_time TIMESTAMP WITH TIME ZONE NOT NULL,
    object_id TEXT NULL,
    object_repr VARCHAR(200) NOT NULL,
    action_flag SMALLINT NOT NULL,
    change_message TEXT NOT NULL,
    content_type_id INTEGER NULL REFERENCES django_content_type(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS django_session (
    session_key VARCHAR(40) PRIMARY KEY,
    session_data TEXT NOT NULL,
    expire_date TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_accessibility_scans_user_id ON accessibility_scans(user_id);
CREATE INDEX IF NOT EXISTS idx_accessibility_scans_created_at ON accessibility_scans(created_at);
CREATE INDEX IF NOT EXISTS idx_alt_text_results_scan_id ON alt_text_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_contrast_results_scan_id ON contrast_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_keyboard_results_scan_id ON keyboard_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_label_results_scan_id ON label_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_table_results_scan_id ON table_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_video_caption_results_scan_id ON video_caption_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_video_autoplay_results_scan_id ON video_autoplay_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_basic_language_results_scan_id ON basic_language_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_markup_error_results_scan_id ON markup_error_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_heading_results_scan_id ON heading_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_response_time_results_scan_id ON response_time_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_pause_control_results_scan_id ON pause_control_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_flashing_results_scan_id ON flashing_results(scan_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_user_id ON feedbacks(user_id);
CREATE INDEX IF NOT EXISTS idx_surveys_user_id ON surveys(user_id);
