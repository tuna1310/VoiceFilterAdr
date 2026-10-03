-- ====================================================================
-- SCHEMA: Voice Filter: Speak Challenge - Remote Content & Configuration
-- ====================================================================

-- 1. Table: Categories / Subjects
CREATE TABLE IF NOT EXISTS categories (
    key VARCHAR(64) PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    title_vi VARCHAR(128) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    learners_count INT NOT NULL DEFAULT 10000,
    card_color VARCHAR(16) NOT NULL,
    accent_color VARCHAR(16) NOT NULL,
    default_game_mode VARCHAR(32) NOT NULL DEFAULT 'voice_runner',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Table: Words
CREATE TABLE IF NOT EXISTS words (
    id VARCHAR(64) PRIMARY KEY,
    word VARCHAR(128) NOT NULL,
    phonetic VARCHAR(128) NOT NULL,
    tip TEXT NOT NULL,
    level VARCHAR(32) NOT NULL CHECK (level IN ('basic', 'intermediate', 'expert')),
    subject VARCHAR(64) NOT NULL REFERENCES categories(key) ON DELETE CASCADE,
    accepted_variants JSONB NOT NULL DEFAULT '[]'::jsonb,
    banner TEXT,
    game_mode VARCHAR(32) NOT NULL DEFAULT 'voice_runner',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Table: Tongue Twisters
CREATE TABLE IF NOT EXISTS tongue_twisters (
    id VARCHAR(64) PRIMARY KEY,
    sentence TEXT NOT NULL,
    phonetic TEXT NOT NULL,
    tip TEXT NOT NULL,
    level VARCHAR(32) NOT NULL CHECK (level IN ('basic', 'intermediate', 'expert')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Table: Content Versions (Semantic Version Tracking for Remote Config)
CREATE TABLE IF NOT EXISTS content_versions (
    key VARCHAR(64) PRIMARY KEY,
    version INT NOT NULL DEFAULT 1,
    notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ====================================================================
-- ROW LEVEL SECURITY (RLS): Read-only access for anonymous users
-- ====================================================================
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE words ENABLE ROW LEVEL SECURITY;
ALTER TABLE tongue_twisters ENABLE ROW LEVEL SECURITY;
ALTER TABLE content_versions ENABLE ROW LEVEL SECURITY;

-- Anonymous SELECT policies
DROP POLICY IF EXISTS "Allow anon read categories" ON categories;
CREATE POLICY "Allow anon read categories" ON categories FOR SELECT TO anon USING (true);

DROP POLICY IF EXISTS "Allow anon read words" ON words;
CREATE POLICY "Allow anon read words" ON words FOR SELECT TO anon USING (true);

DROP POLICY IF EXISTS "Allow anon read tongue_twisters" ON tongue_twisters;
CREATE POLICY "Allow anon read tongue_twisters" ON tongue_twisters FOR SELECT TO anon USING (true);

DROP POLICY IF EXISTS "Allow anon read content_versions" ON content_versions;
CREATE POLICY "Allow anon read content_versions" ON content_versions FOR SELECT TO anon USING (true);

-- ====================================================================
-- SEED DATA
-- ====================================================================

-- Initial Content Version
INSERT INTO content_versions (key, version, notes)
VALUES ('content', 1, 'Initial bundle content version')
ON CONFLICT (key) DO UPDATE SET version = EXCLUDED.version;

-- Seed Categories
INSERT INTO categories (key, title, title_vi, emoji, learners_count, card_color, accent_color, default_game_mode)
VALUES
    ('colors', 'Colors', 'Màu sắc', '🎨', 19740, '#2F80ED', '#1D68C7', 'voice_runner'),
    ('car_brands', 'Car Brands', 'Hãng xe', '🚗', 15516, '#FF7043', '#E64A19', 'voice_runner'),
    ('weekdays', 'Weekdays', 'Ngày trong tuần', '📅', 24110, '#29B6F6', '#0288D1', 'voice_runner'),
    ('months', 'Months', 'Tháng trong năm', '🗓️', 18230, '#AB47BC', '#7B1FA2', 'voice_runner'),
    ('animals', 'Animals', 'Động vật', '🦁', 33224, '#4CAF50', '#2E7D32', 'voice_runner'),
    ('vehicles', 'Vehicles', 'Phương tiện', '🚀', 21890, '#7E57C2', '#512DA8', 'voice_runner'),
    ('kitchen', 'Kitchen Utensils', 'Dụng cụ bếp', '🍳', 12450, '#FFA726', '#F57C00', 'voice_runner'),
    ('countries', 'Countries', 'Quốc gia', '🌍', 32261, '#26A69A', '#00796B', 'voice_runner'),
    ('daily_words', 'Daily Words', 'Từ vựng hàng ngày', '☕', 41200, '#00ACC1', '#00838F', 'voice_runner'),
    ('travel_food', 'Travel & Food', 'Du lịch & Ẩm thực', '✈️', 38900, '#FF7043', '#D84315', 'voice_runner'),
    ('entertainment_sports', 'Movies & Sports', 'Phim ảnh & Thể thao', '🎬', 29500, '#8E24AA', '#6A1B9A', 'voice_runner'),
    ('slang_brainrot', 'Slang & Pop Culture', 'Từ lóng & Giới trẻ', '🔥', 52300, '#FF1744', '#D50000', 'voice_runner')
ON CONFLICT (key) DO NOTHING;
