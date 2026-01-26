-- ============================================
-- 집사형 오버레이 어시스턴트 DB 스키마
-- ============================================

-- 사용자 테이블
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    school_name VARCHAR(100),
    grade INT,
    class_number INT,
    student_number INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_email (email),
    INDEX idx_username (username)
);

-- 집사 캐릭터 마스터 테이블 (사전 정의된 캐릭터 종류)
CREATE TABLE butler_types (
    butler_type_id INT PRIMARY KEY AUTO_INCREMENT,
    type_name VARCHAR(50) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    personality_type VARCHAR(50), -- 예: 정중한, 친근한, 츤데레 등
    default_greeting TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자별 집사 설정 테이블
CREATE TABLE user_butlers (
    user_butler_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    butler_type_id INT NOT NULL,
    butler_name VARCHAR(50), -- 사용자가 지은 집사 이름
    trust_level INT DEFAULT 0, -- 신뢰도
    breathing_level INT DEFAULT 0, -- 호흡도
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (butler_type_id) REFERENCES butler_types(butler_type_id),
    INDEX idx_user_id (user_id)
);

-- 집사 상호작용 로그 (신뢰도/호흡도 계산용)
CREATE TABLE butler_interactions (
    interaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_butler_id BIGINT NOT NULL,
    interaction_type VARCHAR(50) NOT NULL, -- alarm_check, reminder_ignore, task_complete 등
    trust_change INT DEFAULT 0,
    breathing_change INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_butler_id) REFERENCES user_butlers(user_butler_id) ON DELETE CASCADE,
    INDEX idx_user_butler (user_butler_id),
    INDEX idx_created_at (created_at)
);

-- 알람 테이블
CREATE TABLE alarms (
    alarm_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    alarm_type VARCHAR(50) NOT NULL, -- wake_up, school_bell, custom
    alarm_name VARCHAR(100),
    alarm_time TIME NOT NULL,
    days_of_week VARCHAR(20), -- JSON 형태: "[1,2,3,4,5]" (월~금)
    is_active BOOLEAN DEFAULT TRUE,
    snooze_duration INT DEFAULT 5, -- 분 단위
    message TEXT, -- 집사가 전달할 메시지
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_alarm_time (alarm_time)
);

-- 알람 히스토리 (확인 여부 추적)
CREATE TABLE alarm_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alarm_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    triggered_at TIMESTAMP NOT NULL,
    checked_at TIMESTAMP,
    is_checked BOOLEAN DEFAULT FALSE,
    snooze_count INT DEFAULT 0,
    FOREIGN KEY (alarm_id) REFERENCES alarms(alarm_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_triggered_at (triggered_at)
);

-- 시간표 테이블
CREATE TABLE timetables (
    timetable_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    day_of_week INT NOT NULL, -- 1(월) ~ 7(일)
    period INT NOT NULL, -- 교시
    subject VARCHAR(100),
    teacher VARCHAR(50),
    classroom VARCHAR(50),
    start_time TIME,
    end_time TIME,
    semester VARCHAR(20), -- 예: 2024-1, 2024-2
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_day (user_id, day_of_week),
    INDEX idx_semester (semester)
);

-- 일정/리마인더 테이블
CREATE TABLE schedules (
    schedule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    schedule_type VARCHAR(50) NOT NULL, -- exam, assignment, performance_assessment, custom
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL,
    due_time TIME,
    subject VARCHAR(100),
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    reminder_minutes INT DEFAULT 60, -- 몇 분 전 알림
    priority INT DEFAULT 0, -- 우선순위
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_due (user_id, due_date),
    INDEX idx_due_date (due_date)
);

-- 시험 정보 테이블
CREATE TABLE exams (
    exam_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exam_name VARCHAR(100) NOT NULL, -- 중간고사, 기말고사 등
    subject VARCHAR(100) NOT NULL,
    exam_date DATE NOT NULL,
    exam_time TIME,
    exam_room VARCHAR(50),
    duration INT, -- 시험 시간(분)
    syllabus TEXT, -- 시험 범위
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_exam (user_id, exam_date)
);

-- 급식 정보 테이블
CREATE TABLE meals (
    meal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    school_name VARCHAR(100) NOT NULL,
    meal_date DATE NOT NULL,
    meal_type VARCHAR(20) NOT NULL, -- breakfast, lunch, dinner
    menu TEXT NOT NULL,
    calories DECIMAL(6,2),
    allergen_info TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_school_date (school_name, meal_date),
    UNIQUE KEY unique_school_meal (school_name, meal_date, meal_type)
);

-- 학교 공지사항 테이블
CREATE TABLE school_notices (
    notice_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    school_name VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    summary TEXT, -- 집사가 전달할 요약본
    notice_date DATE NOT NULL,
    is_important BOOLEAN DEFAULT FALSE,
    category VARCHAR(50), -- 학사, 급식, 행사 등
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_school_date (school_name, notice_date)
);

-- 사용자별 공지 읽음 상태
CREATE TABLE user_notice_reads (
    read_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notice_id BIGINT NOT NULL,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (notice_id) REFERENCES school_notices(notice_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_notice (user_id, notice_id),
    INDEX idx_user_id (user_id)
);

-- 친구 관계 테이블
CREATE TABLE friendships (
    friendship_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending', -- pending, accepted, blocked
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_friendship (user_id, friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id),
    CHECK (user_id != friend_id)
);

-- 사용자 상태 테이블 (소셜 기능)
CREATE TABLE user_status (
    status_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status_type VARCHAR(50) NOT NULL, -- focusing, sleepy, exam_period, free 등
    status_message VARCHAR(200),
    emoji VARCHAR(10),
    is_visible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- 메신저 대화방 테이블
CREATE TABLE chat_rooms (
    room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_name VARCHAR(100),
    room_type VARCHAR(20) DEFAULT 'direct', -- direct, group
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_created_at (created_at)
);

-- 대화방 참여자 테이블
CREATE TABLE chat_participants (
    participant_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_room_user (room_id, user_id),
    INDEX idx_room_id (room_id),
    INDEX idx_user_id (user_id)
);

-- 메시지 테이블
CREATE TABLE messages (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_text TEXT,
    message_type VARCHAR(20) DEFAULT 'text', -- text, image, system
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_room_created (room_id, created_at),
    INDEX idx_sender_id (sender_id)
);

-- 앱 설정 테이블
CREATE TABLE user_settings (
    setting_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_setting (user_id, setting_key),
    INDEX idx_user_id (user_id)
);

-- 집사 대사 템플릿 테이블
CREATE TABLE butler_dialogue_templates (
    template_id INT PRIMARY KEY AUTO_INCREMENT,
    butler_type_id INT NOT NULL,
    situation VARCHAR(100) NOT NULL, -- wake_up, class_start, exam_reminder 등
    trust_level_min INT DEFAULT 0,
    trust_level_max INT DEFAULT 100,
    dialogue_text TEXT NOT NULL,
    tone VARCHAR(50), -- polite, worried, strict 등
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (butler_type_id) REFERENCES butler_types(butler_type_id),
    INDEX idx_butler_situation (butler_type_id, situation)
);

-- 푸시 알림 로그 테이블
CREATE TABLE notification_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200),
    message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_delivered BOOLEAN DEFAULT FALSE,
    is_opened BOOLEAN DEFAULT FALSE,
    opened_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_sent (user_id, sent_at)
);