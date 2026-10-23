-- ============================================================
-- 校园论坛 点赞/收藏 压测数据
-- 密码统一为 123456（直接复用现有 test 账号的 BCrypt 哈希，避免自己算）
-- 幂等：全部用 INSERT IGNORE，重复执行不会报错
-- ============================================================

-- 1) 150 个压测账号（供最多 150 线程使用；线程数不要超过账号数，
--    否则多个线程共用一个 uid，Redis hash 的 field 会重复，压不出批量落库）
-- 坑：LPAD(n, 2, '0') 在 n>=100 时会“截断”成 2 位（LPAD(100,2,'0')='10'），
--     会跟已有账号撞名被 INSERT IGNORE 丢掉，所以必须用 IF 分支。
INSERT IGNORE INTO db_account (username, email, password, role, register_time, mute, banned)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 150
)
SELECT CONCAT('loadtest', IF(n < 100, LPAD(n, 2, '0'), n)),
       CONCAT('loadtest', IF(n < 100, LPAD(n, 2, '0'), n), '@test.local'),
       (SELECT password FROM db_account WHERE id = 1),
       'user', NOW(), 0, 0
FROM seq;

-- 2) 500 个压测帖子（uid 统一给第一个压测账号；互动压测只关心 tid/uid，不影响结果。内容必须是合法 Quill Delta JSON）
INSERT IGNORE INTO db_topic (id, title, intro, content, uid, time, type, top, locked, invisible)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 500
)
SELECT 1000 + n,
       CONCAT('压测帖子 #', n),
       CONCAT('压测内容 ', n),
       CONCAT('{"ops":[{"insert":"压测帖子内容 ', n, '\\n"}]}'),
       (SELECT MIN(id) FROM db_account WHERE username LIKE 'loadtest%'),
       NOW(), 1, 0, 0, 0
FROM seq;

SELECT '账号数' AS 项目, COUNT(*) AS 数量 FROM db_account WHERE username LIKE 'loadtest%'
UNION ALL
SELECT '帖子数', COUNT(*) FROM db_topic WHERE id >= 1000
UNION ALL
SELECT '帖子 id 范围', MAX(id) FROM db_topic WHERE id >= 1000;
