#!/bin/bash
# 端到端验证主键修复：两个不同用户点赞同一个帖子
BASE=http://localhost:8080
TID=1001

login() {
  for i in 1 2 3; do
    T=$(curl -s -X POST "$BASE/api/auth/login" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$1&password=123456" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    [ -n "$T" ] && { echo "$T"; return; }
    sleep 11
  done
}

export PATH="$PATH:/c/Program Files/MySQL/MySQL Server 8.0/bin"
MYSQL="mysql --default-character-set=utf8mb4 -uroot -p123456 school_froum -N -e"

echo "清理 tid=$TID 的旧数据..."
$MYSQL "DELETE FROM db_topic_interact_like WHERE tid=$TID;" 2>/dev/null

for U in loadtest01 loadtest02 loadtest03; do
  TOKEN=$(login "$U")
  if [ -z "$TOKEN" ]; then echo "$U 登录失败"; continue; fi
  R=$(curl -s -H "Authorization: Bearer $TOKEN" \
      "$BASE/api/forum/interact?tid=$TID&type=like&status=true")
  echo "$U 点赞 -> $R"
done

echo
echo "等 5 秒让 Redis 缓冲落库..."
sleep 5

echo "=== DB 里 tid=$TID 的点赞记录 ==="
$MYSQL "SELECT tid, uid, time FROM db_topic_interact_like WHERE tid=$TID;" 2>/dev/null

echo
echo "=== 用 API 查该帖子的点赞数（应等于 3）==="
TOKEN=$(login loadtest01)
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/forum/topic?tid=$TID" \
  | python -c "import sys,json; d=json.load(sys.stdin)['data']; print('  interactTopic.like =', d['interactTopic']['like'])" 2>/dev/null

echo
echo "=== 帖子列表里的点赞数（原来恒 ≤1）==="
$MYSQL "SELECT tid, COUNT(*) AS 点赞数 FROM db_topic_interact_like WHERE tid=$TID;" 2>/dev/null
