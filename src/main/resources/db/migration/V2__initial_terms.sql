-- Initial terms data (create_initial_terms)
INSERT INTO terms (type, version, change_type, requires_reagreement, change_summary, content, is_active, created_at, updated_at)
SELECT 'service', '1.0', 'major', true, '초기 서비스 이용약관', '<h2>WEBridge 서비스 이용약관</h2><p>본 약관은 WEBridge 서비스 이용에 관한 기본적인 사항을 규정합니다.</p><h3>제1조 (목적)</h3><p>본 약관은 WEBridge가 제공하는 웹 접근성 검사 서비스의 이용조건 및 절차에 관한 사항을 규정함을 목적으로 합니다.</p>', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE type = 'service');

INSERT INTO terms (type, version, change_type, requires_reagreement, change_summary, content, is_active, created_at, updated_at)
SELECT 'privacy', '1.0', 'major', true, '초기 개인정보 처리방침', '<h2>개인정보 수집 및 이용 동의</h2><p>WEBridge는 다음과 같이 개인정보를 수집 및 이용합니다.</p><h3>1. 수집하는 개인정보 항목</h3><ul><li>필수항목: 이메일, 이름</li><li>선택항목: 프로필 이미지</li></ul><h3>2. 수집 및 이용 목적</h3><p>회원 가입 및 관리, 서비스 제공 및 개선</p>', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE type = 'privacy');

INSERT INTO terms (type, version, change_type, requires_reagreement, change_summary, content, is_active, created_at, updated_at)
SELECT 'marketing', '1.0', 'major', true, '초기 마케팅 활용 동의', '<h2>마케팅 목적 개인정보 수집 및 이용 동의</h2><p>WEBridge의 신규 서비스 및 이벤트 정보 안내를 위해 개인정보를 수집 및 이용합니다.</p><h3>1. 수집하는 개인정보 항목</h3><p>이메일, 이름, 서비스 이용 기록</p><h3>2. 이용 목적</h3><p>신규 서비스 안내, 이벤트 및 프로모션 정보 제공</p><h3>3. 보유 기간</h3><p>동의 철회 시까지 또는 회원 탈퇴 시까지</p>', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE type = 'marketing');
