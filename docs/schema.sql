-- JanManch database schema (reference for the project report).
-- Hibernate creates these tables automatically from the entity classes;
-- this file documents the same design in plain SQL.

CREATE TABLE users (
    user_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    created_at     DATETIME(6)  NOT NULL
);

CREATE TABLE channels (
    channel_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(1000),
    created_at   DATETIME(6)  NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE videos (
    video_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id        BIGINT       NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    s3_key            VARCHAR(500),
    thumbnail_key     VARCHAR(500),
    duration_seconds  INT,
    status            VARCHAR(20)  NOT NULL,   -- PROCESSING, READY or FAILED
    view_count        BIGINT       NOT NULL DEFAULT 0,
    uploaded_at       DATETIME(6)  NOT NULL,
    FOREIGN KEY (channel_id) REFERENCES channels(channel_id),
    INDEX idx_videos_channel (channel_id),
    INDEX idx_videos_status_uploaded (status, uploaded_at),
    INDEX idx_videos_status_views (status, view_count)
);

CREATE TABLE video_tags (
    video_id  BIGINT      NOT NULL,
    tag       VARCHAR(50) NOT NULL,
    PRIMARY KEY (video_id, tag),
    FOREIGN KEY (video_id) REFERENCES videos(video_id)
);

CREATE TABLE comments (
    comment_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id    BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    text        VARCHAR(2000) NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    FOREIGN KEY (video_id) REFERENCES videos(video_id),
    FOREIGN KEY (user_id)  REFERENCES users(user_id),
    INDEX idx_comments_video_created (video_id, created_at)
);

CREATE TABLE likes (
    user_id     BIGINT      NOT NULL,
    video_id    BIGINT      NOT NULL,
    type        VARCHAR(10) NOT NULL,   -- LIKE or DISLIKE
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, video_id),
    INDEX idx_likes_video_type (video_id, type)
);

CREATE TABLE subscriptions (
    user_id        BIGINT      NOT NULL,
    channel_id     BIGINT      NOT NULL,
    subscribed_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, channel_id),
    INDEX idx_subscriptions_channel (channel_id)
);

CREATE TABLE watch_history (
    history_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    video_id    BIGINT      NOT NULL,
    watched_at  DATETIME(6) NOT NULL,
    FOREIGN KEY (video_id) REFERENCES videos(video_id),
    INDEX idx_history_user_watched (user_id, watched_at)
);

-- Cardinality of relations:
--   users    1 : 1  channels
--   channels 1 : m  videos
--   videos   1 : m  video_tags
--   videos   1 : m  comments        (users 1 : m comments)
--   users    m : m  videos          through likes
--   users    m : m  channels        through subscriptions
--   users    m : m  videos          through watch_history
