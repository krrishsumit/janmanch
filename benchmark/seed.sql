-- Adds 100,000 READY videos so the feed benchmark has realistic data volume.
-- Run once, after the application has started at least one time (so the tables exist):
--   docker exec -i janmanch-mysql mysql -ujanmanch -pjanmanch_local janmanch_db < benchmark/seed.sql
-- These rows have no video file; they exist only to measure feed and search speed.

SET SESSION cte_max_recursion_depth = 100000;

INSERT INTO users (name, email, password_hash, created_at)
VALUES ('Seed User', 'seed@janmanch.local', 'not-a-real-hash', NOW(6));
SET @user_id = LAST_INSERT_ID();

INSERT INTO channels (user_id, name, description, created_at)
VALUES (@user_id, 'Seed Channel', 'Benchmark data', NOW(6));
SET @channel_id = LAST_INSERT_ID();

INSERT INTO videos (channel_id, title, description, status, view_count, uploaded_at)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT @channel_id,
       CONCAT('Sample video ', n),
       'Seed data for benchmarking',
       'READY',
       FLOOR(RAND() * 100000),
       NOW(6) - INTERVAL n MINUTE
FROM seq;
