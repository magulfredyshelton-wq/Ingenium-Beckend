-- neon_limpar_dados.sql
-- DESTRUCTIVE: deletes every user, post, chat message and file record.
-- Before running: create a Neon branch as a backup (see the steps in the chat).
-- Run the blocks one at a time and read the result of each one.

-- =========================================================
-- 1. LOOK: what tables exist and roughly how many rows each has
-- =========================================================
SELECT relname AS tabela, n_live_tup AS linhas_aprox
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY relname;

-- =========================================================
-- 2. KEEP LIST (edit it if needed). Every other table in "public" is emptied.
--    flyway_schema_history : migration history. NEVER empty it.
--    course, field_study, activity_area : reference data used by sign-up
--    tag_category, tag, course_tag      : tags and weights (the feed needs them)
--    vector_store                       : only if the AI keeps knowledge there. If it stores
--                                         the jobs, remove it from the list and index again later.
-- =========================================================

-- =========================================================
-- 3. WIPE (one single statement: it either works completely or changes nothing)
-- =========================================================
DO $wipe$
DECLARE
tables text;
BEGIN
SELECT string_agg(format('public.%I', tablename), ', ')
INTO tables
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename NOT IN (
                        'flyway_schema_history',
                        'course', 'field_study', 'activity_area',
                        'tag_category', 'tag', 'course_tag',
                        'vector_store'
    );

IF tables IS NULL THEN
        RAISE EXCEPTION 'No tables to empty';
END IF;

    RAISE NOTICE 'Emptying: %', tables;
EXECUTE 'TRUNCATE TABLE ' || tables || ' RESTART IDENTITY CASCADE';
END
$wipe$;

-- =========================================================
-- 4. CHECK: reference data must still be there, user data must be gone
-- =========================================================
SELECT
    (SELECT count(*) FROM tag)          AS tags,           -- 37
    (SELECT count(*) FROM course_tag)   AS course_tag,     -- 77
    (SELECT count(*) FROM course)       AS courses,        -- 16
    (SELECT count(*) FROM usuario)      AS users,          -- 0
    (SELECT count(*) FROM post)         AS posts;          -- 0