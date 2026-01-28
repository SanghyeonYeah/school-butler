CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    profile_image_url TEXT,
    location VARCHAR(100),
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    morning_setup_time TIME DEFAULT '08:00:00',
    night_review_time TIME DEFAULT '22:00:00',
    focus_session_duration INTEGER DEFAULT 25, -- 분 단위
    break_duration INTEGER DEFAULT 5,
    enable_gentle_reminders BOOLEAN DEFAULT TRUE,
    enable_push_notifications BOOLEAN DEFAULT TRUE,
    enable_iot_sync BOOLEAN DEFAULT FALSE,
    character_personality VARCHAR(50) DEFAULT 'friendly', -- friendly, motivating, calm
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);

CREATE TYPE schedule_status AS ENUM ('pending', 'in_progress', 'completed', 'cancelled', 'postponed');
CREATE TYPE schedule_type AS ENUM ('event', 'task', 'routine');

CREATE TABLE schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    schedule_type schedule_type NOT NULL DEFAULT 'event',
    status schedule_status NOT NULL DEFAULT 'pending',
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    all_day BOOLEAN DEFAULT FALSE,
    location VARCHAR(255),
    priority INTEGER DEFAULT 3, -- 1(highest) to 5(lowest)
    tags TEXT[], -- array of tags
    color VARCHAR(7), -- hex color
    reminder_minutes INTEGER[], -- [10, 30, 60] - 시작 전 알림
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_rule JSONB, -- {frequency: 'daily'|'weekly'|'monthly', interval: 1, days: [1,3,5], end_date: '2025-12-31'}
    parent_schedule_id UUID REFERENCES schedules(id) ON DELETE CASCADE, -- 반복 일정의 원본
    original_start_time TIMESTAMP WITH TIME ZONE, -- 반복 일정 수정 시 원래 시간
    completion_note TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schedules_user_id ON schedules(user_id);
CREATE INDEX idx_schedules_start_time ON schedules(start_time);
CREATE INDEX idx_schedules_status ON schedules(status);
CREATE INDEX idx_schedules_type ON schedules(schedule_type);
CREATE INDEX idx_schedules_parent ON schedules(parent_schedule_id);

CREATE TYPE todo_status AS ENUM ('pending', 'in_progress', 'completed', 'cancelled');

CREATE TABLE todos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status todo_status NOT NULL DEFAULT 'pending',
    priority INTEGER DEFAULT 3, -- 1(highest) to 5(lowest)
    due_date DATE,
    due_time TIME,
    estimated_duration INTEGER, -- 예상 소요 시간(분)
    actual_duration INTEGER, -- 실제 소요 시간(분)
    tags TEXT[],
    parent_todo_id UUID REFERENCES todos(id) ON DELETE CASCADE, -- 서브 태스크
    order_index INTEGER DEFAULT 0,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_todos_user_id ON todos(user_id);
CREATE INDEX idx_todos_status ON todos(status);
CREATE INDEX idx_todos_due_date ON todos(due_date);
CREATE INDEX idx_todos_parent ON todos(parent_todo_id);

CREATE TYPE session_status AS ENUM ('active', 'completed', 'cancelled');

CREATE TABLE focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    schedule_id UUID REFERENCES schedules(id) ON DELETE SET NULL,
    todo_id UUID REFERENCES todos(id) ON DELETE SET NULL,
    status session_status NOT NULL DEFAULT 'active',
    planned_duration INTEGER NOT NULL, -- 계획한 시간(분)
    actual_duration INTEGER, -- 실제 시간(분)
    break_count INTEGER DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_focus_sessions_user_id ON focus_sessions(user_id);
CREATE INDEX idx_focus_sessions_started_at ON focus_sessions(started_at);
CREATE INDEX idx_focus_sessions_status ON focus_sessions(status);

CREATE TABLE daily_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_date DATE NOT NULL,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5), -- 1-5 별점
    mood_keyword VARCHAR(50), -- 오늘을 표현하는 한 단어
    reflection TEXT, -- 내일에게 한 마디
    total_focus_minutes INTEGER DEFAULT 0,
    completed_schedules_count INTEGER DEFAULT 0,
    completed_todos_count INTEGER DEFAULT 0,
    total_schedules_count INTEGER DEFAULT 0,
    total_todos_count INTEGER DEFAULT 0,
    achievements JSONB, -- 배지/성취 데이터
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, review_date)
);

CREATE INDEX idx_daily_reviews_user_id ON daily_reviews(user_id);
CREATE INDEX idx_daily_reviews_date ON daily_reviews(review_date);

CREATE TYPE character_activity AS ENUM ('idle', 'focus', 'break', 'notify', 'celebrate');
CREATE TYPE character_emotion AS ENUM ('normal', 'happy', 'proud', 'tired', 'worried', 'excited');

CREATE TABLE character_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity character_activity NOT NULL DEFAULT 'idle',
    emotion character_emotion NOT NULL DEFAULT 'normal',
    message TEXT,
    led_color VARCHAR(7), -- IoT LED 색상 (hex)
    led_pattern VARCHAR(50), -- IoT LED 패턴 (solid, blink, pulse)
    animation_key VARCHAR(100), -- 앱 애니메이션 키
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_character_states_user_id ON character_states(user_id);
CREATE INDEX idx_character_states_created_at ON character_states(created_at);

CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id UUID NOT NULL, -- 대화 세션 ID
    user_message TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    intent VARCHAR(100), -- 의도 분류: plan_creation, schedule_adjustment, motivational, etc.
    context_data JSONB, -- 대화 시 참조한 일정/할일 데이터
    model_used VARCHAR(50) DEFAULT 'gemini-1.5-flash',
    tokens_used INTEGER,
    response_time_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_session_id ON ai_conversations(session_id);
CREATE INDEX idx_ai_conversations_created_at ON ai_conversations(created_at);

CREATE TYPE notification_type AS ENUM ('reminder', 'morning_setup', 'night_review', 'focus_start', 'focus_end', 'achievement');
CREATE TYPE notification_status AS ENUM ('pending', 'sent', 'failed', 'cancelled');

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    schedule_id UUID REFERENCES schedules(id) ON DELETE CASCADE,
    todo_id UUID REFERENCES todos(id) ON DELETE CASCADE,
    notification_type notification_type NOT NULL,
    status notification_status NOT NULL DEFAULT 'pending',
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    fcm_message_id VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_scheduled_at ON notifications(scheduled_at);
CREATE INDEX idx_notifications_status ON notifications(status);

CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_name VARCHAR(100) NOT NULL,
    event_category VARCHAR(50),
    properties JSONB,
    session_id UUID,
    device_info JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_events_user_id ON analytics_events(user_id);
CREATE INDEX idx_analytics_events_name ON analytics_events(event_name);
CREATE INDEX idx_analytics_events_created_at ON analytics_events(created_at);