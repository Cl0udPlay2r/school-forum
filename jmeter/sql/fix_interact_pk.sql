-- ============================================================
-- 修复互动表主键缺陷
-- 问题：PRIMARY KEY (tid) 导致 INSERT IGNORE 时，同一帖子的第 2..N 个
--       用户的点赞/收藏记录因主键冲突被静默丢弃，每个帖子最多只能存 1 行。
-- 现象：SELECT COUNT(*) FROM db_topic_interact_like 恒等于帖子数；
--       帖子点赞数永远 ≤ 1；非首个用户的点赞刷新后消失。
-- 修复：改为复合主键 (tid, uid)，与原有 UNIQUE KEY tid_uid_like 的语义一致。
-- 注意：Java 代码里 insert ignore / delete / count 全部是按 (tid,uid) 写的，
--       所以修完不需要改任何代码。
-- ============================================================

-- ---------- 0) 安全检查：uid 不能有 NULL（复合主键要求 NOT NULL）----------
SELECT 'like 表中 uid 为 NULL 的行数' AS 检查项, COUNT(*) AS 数量
FROM db_topic_interact_like WHERE uid IS NULL
UNION ALL
SELECT 'collect 表中 uid 为 NULL 的行数', COUNT(*)
FROM db_topic_interact_collect WHERE uid IS NULL;

-- ---------- 1) 备份 ----------
DROP TABLE IF EXISTS db_topic_interact_like_bak;
DROP TABLE IF EXISTS db_topic_interact_collect_bak;
CREATE TABLE db_topic_interact_like_bak    AS SELECT * FROM db_topic_interact_like;
CREATE TABLE db_topic_interact_collect_bak AS SELECT * FROM db_topic_interact_collect;

-- ---------- 2) 修复主键 ----------
ALTER TABLE db_topic_interact_like
    DROP PRIMARY KEY,
    DROP INDEX tid_uid_like,
    MODIFY uid int NOT NULL,
    ADD PRIMARY KEY (tid, uid);

ALTER TABLE db_topic_interact_collect
    DROP PRIMARY KEY,
    DROP INDEX tid_uid_collect,
    MODIFY uid int NOT NULL,
    ADD PRIMARY KEY (tid, uid);

-- ---------- 3) 验证 ----------
SELECT '=== 修复后的表结构 ===' AS '';
SHOW CREATE TABLE db_topic_interact_like\G
SHOW CREATE TABLE db_topic_interact_collect\G

SELECT '=== 现在每个 tid 可以有多行了（用测试账号验证）===' AS '';
SELECT tid, COUNT(*) AS 行数 FROM db_topic_interact_like GROUP BY tid ORDER BY 2 DESC LIMIT 3;
