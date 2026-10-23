# 校园论坛点赞/收藏接口压测与复现

目标接口：`GET /api/forum/interact?tid=&type=&status=`

压测计划：`点赞压测-v2.jmx`（JMeter 5.6.3）
分析脚本：`analyze_jtl.py`、`summarize_results.py`
结果摘要：`result/SUMMARY.md`

> 原始 JTL 合计约 434 MB，保留在本地，不提交 Git；入库的是摘要、JMX、脚本和造数 SQL。

---

## 一、结论

同一台机器、同一份 JMX、同一份数据集、50 线程：

| 阶段 | 关键改动 | 稳态 TPS | 平均 RT | P99 |
|---|---|---:|---:|---:|
| 基线 | 修复测量方法后的基线 | 1,050.7 | 46.8 ms | 121 ms |
| ① | IP 限流 Lua 原子化 + 封禁状态 Caffeine 缓存 | 3,307.2 | 14.8 ms | 57 ms |
| ② | Redis `RENAME` 原子 drain 替代 `synchronized(type.intern())` | 4,852.3 | 9.6 ms | 16 ms |
| ③ | 批量落库 SQL 改 `(tid, uid) IN` + 500 行分批 | 5,305.6 | 8.7 ms | 16 ms |
| ④ | Lettuce 连接池 + 恢复完整 C2 JIT | 6,490.2 | 6.9 ms | 10 ms |

- 终态相对基线：**6.18×（+517.7%）**。
- 纯代码路径（①-③，不含连接池与 C2）：**5.05×（+405.0%）**。
- 全部轮次 `success=false` 为 0；JTL 的成功判定来自 JSON 断言 `$.code == 200`。

50 → 100 线程对照：6,490.2 → 6,288.3 TPS，平均 RT 6.9 → 14.6 ms，已经过了吞吐拐点。

双源 IP 实验：单 IP 基线 1,050.7 TPS → 两个 JMeter 实例合计 1,710.9 TPS（+62.8%），指向按 IP 加的 `synchronized(address.intern())`。

---

## 二、环境与数据

### 2.1 环境

| 项 | 值 |
|---|---|
| 机器 | i7-12700H，14 核 / 20 逻辑核，23.7 GB RAM |
| 系统 | Windows 11 |
| JDK | Java 17.0.12 |
| MySQL | 8.0.42 |
| JMeter | 5.6.3 |
| 网络 | 后端与 JMeter 同机，loopback |

### 2.2 数据集

| 数据 | 数量 | 说明 |
|---|---:|---|
| 压测账号 | 50（`accounts.csv` 共 150） | `loadtest01` ~ `loadtest50`，密码 `123456` |
| 压测帖子 | 500 | id `1001` ~ `1500` |
| 参数组合 | 5 行循环 | like 60% / collect 40%；true 60% / false 40% |

`accounts.csv` 现在有 150 个账号，本次实验只用前 50 个。**线程数不要超过账号数**，否则多个线程共用一个 uid，Redis 的 `tid:uid` field 会重复，压不出批量落库。

`loadtest_data.sql` 会创建 150 个账号和 500 个帖子。它从 `db_account` 里 `id = 1` 的账号复制 BCrypt 密码，所以执行前要先注册一个账号（或保证 id=1 存在）。帖子归属不影响本压测，互动只关心 `(tid, uid)`。

---

## 三、怎么复现

### 3.1 准备

1. 导入 `docs/schema.sql` 建库。
2. 启动 MySQL、Redis、RabbitMQ、MinIO 和后端。
3. 注册一个账号（确保 `db_account.id = 1` 存在）。
4. 执行造数：

```bash
mysql --default-character-set=utf8mb4 -uroot -p123456 school_froum < jmeter/sql/loadtest_data.sql
```

5. 确认 `application-dev.yml` 里 IP 限流阈值已放宽：

```yaml
spring:
  web:
    flow:
      period: 3
      limit: 10000000   # 压测时避免 429；生产默认 10
      block: 30
```

放宽阈值只是避免 429，**过滤器仍会对每个请求计数**；所以双 IP 实验测的是锁/Redis 往返，不是“因为被限流才慢”。

### 3.2 运行

```bash
jmeter -n -t jmeter/点赞压测-v2.jmx -l jmeter/result/run.jtl \
  -JTHREADS=50 -JRAMP=5 -JDURATION=60 \
  -JTID_MIN=1001 -JTID_MAX=1500
```

JMX 默认值：`THREADS=50`、`RAMP=5`、`DURATION=60`、`WARMUP_THREADS=20`、`WARMUP_DURATION=20`、`TID_MIN=1001`、`TID_MAX=1500`。

### 3.3 分析

```bash
python jmeter/analyze_jtl.py jmeter/result/run.jtl
python jmeter/summarize_results.py
```

`analyze_jtl.py` 会剔除 `W*` 预热样本、用 `allThreads` 识别稳态区间、计算 P50/P90/P95/P99 和 Little's Law 并发；`summarize_results.py` 生成 `result/SUMMARY.md`。

---

## 四、JMX 里几个关键设计

1. **预热线程组**：20 线程 × 20s，请求名以 `W` 开头，统计时剔除。
2. **每线程只登录一次**：登录放在 `Once Only Controller`，避免每次请求多一次往返，也避免撞 JWT 登录限流。
3. **JSON 断言 `$.code == 200`**：项目鉴权失败返回 HTTP 200 + JSON body 里的 401/403；只看 HTTP 200 会得到 100% 成功的假报告。
4. **两个 CSV**：`accounts.csv`（账号）和 `interact_params.csv`（type/status 组合），均为 `shareMode.all`。
5. **tid 随机**：`${__Random(TID_MIN,TID_MAX)}`；切换热点/长尾只改两个参数。
6. **JMeter 变量线程私有**：登录必须和主请求在同一线程组；放在 setUp 线程组里，主线程组读不到 token。

---

## 五、瓶颈定位时间线

1. **先修测量方法**：JMeter GUI 监听器在采样线程里同步通知、监听器累积、无预热，同配置两次差 30%；改用 `jmeter -n` 新进程 + 预热 + 稳态，三次差异降到 ±1%。
2. **确认是服务端**：基线 1,050.7 TPS，稳态并发 49.2/50（Little's Law）；50→100 线程 TPS 基本不变、RT 接近翻倍；CPU 约 25%；两个 JMeter 实例可以叠加吞吐。
3. **IP 限流锁**：原实现 `synchronized(address.intern())` 包住「查封禁 + 取计数 + 自增」3 次 Redis 往返。双 IP 实验第一次只改目标地址，Windows 仍用 127.0.0.1 当源地址，实验无效；第二次用 `HTTPSampler.ipSource` 显式绑定源地址，并用 Redis 键 `flow:counter:127.0.0.2` 验证。修法：Lua 原子限流 + Caffeine 封禁缓存。
4. **type 锁**：`jstack` 30 份快照、2,483 个 HTTP 线程样本，63.2% BLOCKED，栈顶是 `TopicServiceImpl.interact`；JTL 每秒最大 RT 也有 3 秒周期。修法：`RENAME` 把增量原子搬走，新写入落到新 key，落库不再需要 Java 锁；BLOCKED 降到 0%。
5. **批量删除 SQL 退化**：`(tid=.. and uid=..) or ...` 上千个 OR，超过 `range_optimizer_max_mem_size`（默认 8MB）后退化成逐条件全表扫描。同一张 3.3 万行表、18,000 个条件：OR 链 91,000 ms，`(tid,uid) IN` 418 ms（218×）；2,900 个条件时还是 0 秒，是断崖式退化。修法：tuple IN + 500 行分批。
6. **Redis 单连接**：44.1% 线程栈在 `io.lettuce.core.protocol.AsyncCommand.await`，`CLIENT LIST` 只有 1 条连接；加连接池后 1 → 9 条，100 线程不再比 50 线程更慢。叠加恢复 C2 JIT。
7. **数据校验**：预期 50 账号 × 500 帖 = 25,000 行，实测只有 501 行；`PRIMARY KEY (tid)` + `UNIQUE KEY (tid, uid)` 语义矛盾，`INSERT IGNORE` 把第二个用户的点赞静默丢弃。修法：复合主键 `(tid, uid)`，Java 不用改。
8. **启动阻塞**：TPS 涨上去后，慢 SQL 把启动补偿阻塞在 `@PostConstruct`，Tomcat 起不来。日志中补偿开始到 Tomcat 启动约 158–176s；修法：移到 `ApplicationReadyEvent`。

<details>
<summary>启动阻塞的日志摘录</summary>

```text
16:40:14 WARN  c.example.service.impl.TopicServiceImpl : 检测到上次未落库的[like]互动数据，正在补偿写入数据库
16:43:10 INFO  o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port 8080 (http)
```

</details>

---

## 六、压测中顺带修掉的问题

| # | 缺陷 | 现象 | 根因 | 修复 |
|---|---|---|---|---|
| 1 | 互动表主键只有 `tid` | 预期 25,000 行，实测 501 行 | `PRIMARY KEY(tid)` + `INSERT IGNORE` 静默丢弃 | `PRIMARY KEY(tid, uid)` |
| 2 | 登录限流恒拒 | 同一账号 10s 内无法二次登录 | lambda 无条件 `return false` | `return !overclock` |
| 3 | 列表缓存永不失效 | 删帖/屏蔽后列表仍显示 | 失效时删错 key 前缀 | 统一删 `topic:preview:*` |
| 4 | 详情页 500 | 压测账号缺少 details/privacy 行时 NPE | 未做空值兜底 | 空对象/空数组兜底 |
| 5 | IP 限流完全失效 | 所有请求都不限流 | 条件只对 `OPTIONS` 生效 | 方法白名单 + `!tryCount` |
| 6 | 启动阻塞约 160s | 补偿先于 Tomcat 启动 | `@PostConstruct` + 慢 SQL | 移到 `ApplicationReadyEvent` |
| 7 | 批量删除 91s | 落库毛刺 / 慢 SQL | 上千 OR 条件超 8MB | `(tid,uid) IN` + 500 行分批 |

主键修复脚本：`sql/fix_interact_pk.sql`；端到端验证脚本：`sql/verify_pk.sh`。

---

## 七、口径与边界

- TPS/RT/P99 全部来自 JTL 稳态区间；不是 JMeter 聚合报告的 GUI 数字。
- 原始 JTL 约 434 MB，不提交；`result/SUMMARY.md` 是唯一入库的统计摘要。
- jstack、`CLIENT LIST`、SQL 91s/418ms、启动耗时是当次手工记录，不在 JTL 里。
- 基线 P99 复算是 121 ms（不是 102 ms）；全量含 ramp-up 的 P99 是 119 ms。
- 同机 loopback、20 逻辑核、单实例；换机器/分机部署的净效应需要实测。
- `6,490` 含连接池和恢复 C2；纯代码路径是 5,305.6。

---

## 八、文件说明

| 文件 | 作用 |
|---|---|
| `点赞压测-v2.jmx` | 稳态压测计划（预热 + 主线程组） |
| `data/accounts.csv` | 150 个压测账号 |
| `data/interact_params.csv` | like/collect × true/false 组合 |
| `sql/loadtest_data.sql` | 造 150 账号 + 500 帖 |
| `sql/fix_interact_pk.sql` | 互动表复合主键修复 |
| `sql/verify_pk.sh` | 三个用户给同一帖点赞的端到端验证 |
| `analyze_jtl.py` | 单文件详细分析（概览/分位数/时间线/周期） |
| `summarize_results.py` | 多文件阶段对比，生成 `result/SUMMARY.md` |
| `result/SUMMARY.md` | 入库的压测摘要 |
| `result/*.jtl` | 原始数据，本地保留，Git 忽略 |
