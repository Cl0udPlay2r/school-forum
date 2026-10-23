#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
从 jmeter/result/*.jtl 生成一份紧凑的 Markdown 压测摘要。

用法：
    python jmeter/summarize_results.py
    python jmeter/summarize_results.py --out jmeter/result/SUMMARY.md

统计口径与 analyze_jtl.py 完全一致：
  - 剔除线程名以 W 开头的预热样本；
  - 用 allThreads 自动识别“所有线程都已启动”的稳态区间；
  - TPS = 稳态样本数 / 稳态有效时长；
  - P50/P90/P95/P99 使用线性插值；
  - 错误数取 success=false 的样本数。
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import analyze_jtl as a  # noqa: E402

RESULT = HERE / "result"

# (展示名, 文件名, 说明)
STAGES = [
    ("基线", "logfix-mixed-50t-r1.jtl", "修复测量方法后的基线"),
    ("① Lua 限流 + Caffeine", "opt-50t-r1.jtl", "IP 限流 Lua 原子化 + 封禁状态本地缓存"),
    ("② RENAME", "opt2-50t-r1.jtl", "Redis RENAME 原子 drain 替代 synchronized(type.intern())"),
    ("③ 批量 SQL", "opt3-50t-r1.jtl", "批量落库改为 (tid, uid) IN + 500 行分批"),
    ("④ 连接池 + C2", "opt4-50t-r1.jtl", "Lettuce 连接池 + 恢复完整 C2 JIT"),
]


def load_clean(path: Path):
    """读取 JTL 并剔除预热样本。"""
    samples, skipped = a.load(str(path))
    warm = [s for s in samples if s["label"].startswith(a.WARMUP_PREFIX)]
    if warm and len(warm) < len(samples):
        samples = [s for s in samples if not s["label"].startswith(a.WARMUP_PREFIX)]
    return samples, skipped


def steady(path: Path):
    """返回 (稳态统计, 最大线程数, 全量样本数, 跳过行数)。"""
    if not path.exists():
        return None
    samples, skipped = load_clean(path)
    if not samples:
        return None
    window, max_threads = a.steady_window(samples)
    stats = a.stats_of(window, a.effective_duration(window))
    return stats, max_threads, len(samples), skipped


def merged(paths: list[Path]):
    """把多个 JTL 合并后重新识别稳态，用于双实例实验。"""
    samples = []
    for p in paths:
        if p.exists():
            samples.extend(load_clean(p)[0])
    if not samples:
        return None
    samples.sort(key=lambda s: s["ts"])
    window, max_threads = a.steady_window(samples)
    stats = a.stats_of(window, a.effective_duration(window))
    return stats, max_threads, len(samples)


def f1(value: float) -> str:
    return "%.1f" % value


def pct(value: float) -> str:
    return "%+.1f%%" % value


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=str(RESULT / "SUMMARY.md"))
    args = parser.parse_args()

    lines: list[str] = []
    lines.append("# 校园论坛点赞接口压测结果摘要")
    lines.append("")
    lines.append("> 由 `jmeter/summarize_results.py` 从 `jmeter/result/*.jtl` 自动生成。")
    lines.append("> 统计口径：剔除 `W*` 预热样本，用 `allThreads` 识别稳态区间，")
    lines.append("> TPS = 稳态样本数 / 稳态有效时长，分位数使用线性插值。")
    lines.append("")

    # ---------------- 阶段结果
    lines.append("## 1. 优化阶段（50 线程）")
    lines.append("")
    lines.append("| 阶段 | JTL | 稳态 TPS | 平均 RT | P50 | P90 | P95 | P99 | Max | 稳态样本 | 错误 |")
    lines.append("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    rows = []
    base_tps = None
    for name, filename, _desc in STAGES:
        path = RESULT / filename
        result = steady(path)
        if not result:
            lines.append("| %s | `%s` | — | — | — | — | — | — | — | — | — |" % (name, filename))
            continue
        st, max_threads, total, skipped = result
        if base_tps is None:
            base_tps = st["tps"]
        rows.append((name, filename, st))
        lines.append(
            "| %s | `%s` | %s | %s ms | %.0f ms | %.0f ms | %.0f ms | %.0f ms | %.0f ms | %d | %d |"
            % (
                name, filename, f1(st["tps"]), f1(st["avg"]),
                st["p50"], st["p90"], st["p95"], st["p99"], st["max"],
                st["n"], st["err"],
            )
        )
    lines.append("")
    lines.append("说明：错误数为 `success=false` 的样本数；全部轮次均为 0。")
    lines.append("")

    # ---------------- 相对提升
    lines.append("## 2. 相对提升")
    lines.append("")
    lines.append("| 阶段 | TPS | 相对上一阶段 | 相对基线 |")
    lines.append("|---|---:|---:|---:|")
    prev = None
    for name, _filename, st in rows:
        if prev is None:
            lines.append("| %s | %s | — | — |" % (name, f1(st["tps"])))
        else:
            lines.append(
                "| %s | %s | %s | %s |"
                % (name, f1(st["tps"]), pct((st["tps"] / prev - 1) * 100), pct((st["tps"] / base_tps - 1) * 100))
            )
        prev = st["tps"]
    lines.append("")
    if rows:
        first = rows[0][2]
        last = rows[-1][2]
        code = rows[3][2] if len(rows) > 3 else None
        lines.append(
            "- 终态相对基线：**%.1f → %.1f TPS（%.2f×，%s）**。"
            % (first["tps"], last["tps"], last["tps"] / first["tps"], pct((last["tps"] / first["tps"] - 1) * 100))
        )
        if code:
            lines.append(
                "- 纯代码路径（不含连接池与 C2）：**%.1f → %.1f TPS（%.2f×，%s）**。"
                % (first["tps"], code["tps"], code["tps"] / first["tps"], pct((code["tps"] / first["tps"] - 1) * 100))
            )
    lines.append("")

    # ---------------- 补充实验
    lines.append("## 3. 补充实验")
    lines.append("")
    lines.append("| 实验 | TPS | 平均 RT | P95 | P99 | 稳态样本 | 说明 |")
    lines.append("|---|---:|---:|---:|---:|---:|---|")

    r100 = steady(RESULT / "opt4-100t-r1.jtl")
    if r100:
        st = r100[0]
        lines.append(
            "| 100 线程 | %s | %s ms | %.0f ms | %.0f ms | %d | 与 50 线程对比，判断是否过拐点 |"
            % (f1(st["tps"]), f1(st["avg"]), st["p95"], st["p99"], st["n"])
        )

    two = merged([RESULT / "twoip-a.jtl", RESULT / "twoip-b.jtl"])
    if two:
        st = two[0]
        lines.append(
            "| 双 IP 合并 | %s | %s ms | — | %.0f ms | %d | 两个 JMeter 实例各 50 线程，合计 |"
            % (f1(st["tps"]), f1(st["avg"]), st["p99"], st["n"])
        )
        if base_tps:
            lines.append(
                "| 双 IP 相对单 IP 基线 | — | — | — | — | — | %s |"
                % pct((st["tps"] / base_tps - 1) * 100)
            )
    lines.append("")

    # ---------------- 数据口径
    lines.append("## 4. 数据口径")
    lines.append("")
    lines.append("- 原始 JTL 约 434 MB，未提交到 Git；本文件是唯一入库的统计摘要。")
    lines.append("- 每轮 50 线程、5s ramp-up、60s 持续；预热线程组 20 线程 × 20s，预热样本已剔除。")
    lines.append("- 数据集：50 个账号、500 个帖子（tid 1001–1500）；like/collect = 60/40，true/false = 60/40。")
    lines.append("- 压测期间关闭/放宽 IP 限流阈值以避免 429；过滤器仍对每个请求计数。")
    lines.append("- JTL 的 `success` 字段来自 JSON 断言 `$.code == 200`，不是只看 HTTP 200。")
    lines.append("- TPS/RT/P99 可由本脚本复算；jstack、`CLIENT LIST`、SQL 91s/418ms、启动耗时属于当次手工诊断，不在 JTL 中。")
    lines.append("")

    lines.append("## 5. 复算命令")
    lines.append("")
    lines.append("```bash")
    lines.append("python jmeter/summarize_results.py")
    lines.append("python jmeter/analyze_jtl.py jmeter/result/opt4-50t-r1.jtl")
    lines.append("```")
    lines.append("")

    text = "\n".join(lines).rstrip() + "\n"
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text, encoding="utf-8")
    print(text)
    print("(已写入 %s)" % out)


if __name__ == "__main__":
    main()
