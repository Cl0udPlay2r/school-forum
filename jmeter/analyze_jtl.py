#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
JMeter JTL 分析脚本（只依赖标准库）

用法：
    python analyze_jtl.py ../result/longtail-50t.jtl
    python analyze_jtl.py a.jtl b.jtl c.jtl          # 多文件自动出对比表
    python analyze_jtl.py a.jtl --out report.md       # 同时写一份到文件

解析的内容（设计思路，讲给别人听也能用）：
  1. 概览           —— 总样本 / 时长 / 总 TPS / 错误率
  2. 分请求统计     —— 平均值会骗人，所以给 P50/P90/P95/P99
  3. 平均并发       —— Little's Law: 并发 = TPS × 响应时间，用来判断"到底压上去了没有"
  4. 稳态区间       —— 用 JTL 里的 allThreads 自动识别"所有线程都已启动"的区间，
                       把 ramp-up 爬坡段从统计里剔掉
  5. 每秒时间线     —— ASCII 图，直接看爬坡、平台期、抖动
  6. 周期性毛刺检测 —— 找出 RT 尖峰，统计尖峰间隔是否接近 3 秒（验证"延迟落库阻塞在线请求"的假设）
  7. 参数分布       —— 从 URL 里统计 tid 分布，验证测试数据集是不是真的"热点/长尾"
"""
import csv
import math
import os
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

FIELDS = ["timeStamp", "elapsed", "label", "responseCode", "responseMessage", "threadName",
          "dataType", "success", "failureMessage", "bytes", "sentBytes", "grpThreads",
          "allThreads", "URL", "Latency", "IdleTime", "Connect"]

# 预热线程组里的请求名以 W 开头。CLI 模式(-l)下连预热的数据也会写进 JTL，
# 统计前必须剔除，否则会把冷启动阶段和正式压测混在一起。
WARMUP_PREFIX = "W"


# ---------------------------------------------------------------- 基础工具
def percentile(sorted_vals, p):
    """线性插值分位数"""
    if not sorted_vals:
        return 0.0
    if len(sorted_vals) == 1:
        return float(sorted_vals[0])
    k = (len(sorted_vals) - 1) * p / 100.0
    f = int(math.floor(k))
    c = min(f + 1, len(sorted_vals) - 1)
    return sorted_vals[f] + (sorted_vals[c] - sorted_vals[f]) * (k - f)


def bar(value, unit, width=36):
    """把数值画成 ASCII 条"""
    n = int(round(value / unit)) if unit else 0
    return "█" * min(n, width)


def quantize_url(url):
    """从 URL 里抽出 tid 参数，用于统计参数分布"""
    if "tid=" not in url:
        return None
    try:
        part = url.split("tid=", 1)[1]
        for sep in ("&", "#"):
            part = part.split(sep, 1)[0]
        return int(part)
    except Exception:
        return None


# ---------------------------------------------------------------- 读取
def _num(v, default=0):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def load(path):
    """
    读 JTL。
    坑：JMeter 写文件时，saveAssertionResultsFailureMessage=false 的表头只有 16 列（没有
    failureMessage），但失败样本的行依然是 17 列。所以不能信表头，必须按 JMeter 固定的列顺序
    做对齐：17 列直接用，16 列则在第 8 位补一个空 failureMessage。
    """
    samples = []
    skipped = 0
    with open(path, newline="", encoding="utf-8", errors="replace") as f:
        reader = csv.reader(f)
        first = next(reader, None)
        # 如果第一行不是表头，把它当数据一起处理
        if first and first[0].strip().isdigit():
            rows = [first]
        else:
            rows = []
        for row in list(rows) + [r for r in reader]:
            if not row:
                continue
            if len(row) == len(FIELDS):
                rec = dict(zip(FIELDS, row))
            elif len(row) == len(FIELDS) - 1:
                rec = dict(zip(FIELDS, row[:8] + [""] + row[8:]))
            else:
                skipped += 1
                continue
            try:
                ts = int(rec["timeStamp"])
                el = float(rec["elapsed"])
            except (TypeError, ValueError, KeyError):
                skipped += 1
                continue
            ok = str(rec.get("success", "")).strip().lower() == "true"
            samples.append({
                "ts": ts,
                "el": el,
                "label": (rec.get("label") or "?").strip(),
                "ok": ok,
                "code": (rec.get("responseCode") or "").strip(),
                "msg": (rec.get("failureMessage") or "").strip(),
                "url": (rec.get("URL") or "").strip(),
                "bytes": int(_num(rec.get("bytes"))),
                "all": int(_num(rec.get("allThreads"))),
            })
    samples.sort(key=lambda s: s["ts"])
    return samples, skipped


# ---------------------------------------------------------------- 统计分析
def segments(samples):
    """
    JMeter 的 ResultCollector 在 GUI 模式下是追加写文件的，所以同一个 .jtl 里可能拼接了多次运行。
    这里按“秒”切片，把中间的空档识别出来，得到真正的有效运行时长。
    """
    if not samples:
        return []
    buckets = sorted(set(int(s["ts"] / 1000) for s in samples))
    segs = []
    start = prev = buckets[0]
    for b in buckets[1:]:
        if b - prev > 1:
            segs.append((start, prev))
            start = b
        prev = b
    segs.append((start, prev))
    return segs


def effective_duration(samples):
    """有效时长 = 各运行片段长度之和（不把运行之间的空档算进去）"""
    return max(1.0, float(sum(e - s + 1 for s, e in segments(samples))))


def stats_of(samples, duration_s=None):
    els = sorted(s["el"] for s in samples)
    n = len(els)
    if n == 0:
        return None
    if duration_s is None:
        span_ms = (samples[-1]["ts"] + samples[-1]["el"]) - samples[0]["ts"]
        duration_s = max(span_ms / 1000.0, 0.001)
    tps = n / duration_s
    avg = sum(els) / n
    errs = sum(1 for s in samples if not s["ok"])
    return {
        "n": n,
        "span": duration_s,
        "tps": tps,
        "avg": avg,
        "min": els[0],
        "p50": percentile(els, 50),
        "p90": percentile(els, 90),
        "p95": percentile(els, 95),
        "p99": percentile(els, 99),
        "max": els[-1],
        "err": errs,
        "err_pct": errs * 100.0 / n,
        # Little's Law：平均并发请求数 = 吞吐量 × 平均响应时间
        "concurrency": tps * avg / 1000.0,
    }


def steady_window(samples):
    """用 allThreads 找出"全部线程都已启动"的区间，剔除 ramp-up"""
    max_threads = max((s["all"] for s in samples), default=0)
    if max_threads <= 0:
        # 没有 allThreads 列时，退化成掐头去尾各 10%
        return samples[len(samples) // 10: len(samples) - len(samples) // 10], None
    started = [s for s in samples if s["all"] >= max_threads]
    if len(started) < 10:
        return samples, max_threads
    lo, hi = started[0]["ts"], started[-1]["ts"]
    return [s for s in samples if lo <= s["ts"] <= hi], max_threads


def timeline(samples, t0):
    """按秒聚合"""
    buckets = {}
    for s in samples:
        sec = int((s["ts"] - t0) / 1000.0)
        b = buckets.setdefault(sec, {"n": 0, "sum": 0.0, "max": 0.0, "err": 0})
        b["n"] += 1
        b["sum"] += s["el"]
        b["max"] = max(b["max"], s["el"])
        if not s["ok"]:
            b["err"] += 1
    out = []
    for sec in sorted(buckets):
        b = buckets[sec]
        out.append({"sec": sec, "n": b["n"], "avg": b["sum"] / b["n"],
                    "max": b["max"], "err": b["err"]})
    return out


def latency_pattern(tl):
    """
    分析每秒「最大 RT」的时间结构 —— 用来区分两种完全不同的性能问题：
      A. 持续性尾部延迟：每秒最大 RT 的中位数本身就很高 -> 临界区（锁内操作）长期饱和排队
      B. 周期性毛刺    ：只有某些秒特别高     -> 周期性任务（这里是 3 秒一次的批量落库）阻塞在线请求

    判定 B 用自相关：在最长连续片段上算 lag=1..6 的自相关系数。
    如果是 3 秒周期，lag=3 及其谐波 lag=6 会明显为正，而 lag=1/2/4/5 偏负。
    注意：不能拿「Top N 秒的间隔」去判周期，那会被多次运行之间的空档污染（我踩过这个坑）。
    """
    if len(tl) < 12:
        return None
    segs = []
    start = prev = tl[0]["sec"]
    for t in tl[1:]:
        if t["sec"] - prev > 1:
            segs.append((start, prev))
            start = t["sec"]
        prev = t["sec"]
    segs.append((start, prev))
    lo, hi = max(segs, key=lambda s: s[1] - s[0])
    seq = [t["max"] for t in tl if lo <= t["sec"] <= hi]
    avgseq = [t["avg"] for t in tl if lo <= t["sec"] <= hi]
    n = len(seq)
    if n < 12:
        return None

    def ac(x, lag):
        m = sum(x) / len(x)
        num = sum((x[i] - m) * (x[i + lag] - m) for i in range(len(x) - lag))
        den = sum((v - m) ** 2 for v in x)
        return num / den if den else 0.0

    acs = {lag: ac(seq, lag) for lag in range(1, 7)}
    srt = sorted(seq)
    period = 3 if (acs[3] > 0.2 and acs[3] > max(acs[1], acs[2], acs[4], acs[5])) else None
    return {
        "seg": (lo, hi),
        "n": n,
        "p50_max": percentile(srt, 50),
        "p90_max": percentile(srt, 90),
        "max_max": srt[-1],
        "p50_avg": percentile(sorted(avgseq), 50),
        "acs": acs,
        "period": period,
    }


# ---------------------------------------------------------------- 报告
def report(path):
    lines = []
    samples, skipped = load(path)
    warm = [s for s in samples if s["label"].startswith(WARMUP_PREFIX)]
    if warm and len(warm) < len(samples):
        samples = [s for s in samples if not s["label"].startswith(WARMUP_PREFIX)]
    else:
        warm = []
    if not samples:
        return ["### %s\n（文件为空或格式无法识别）\n" % os.path.basename(path)], None

    t0 = samples[0]["ts"]
    segs = segments(samples)
    eff = effective_duration(samples)
    overall = stats_of(samples, eff)
    steady, max_threads = steady_window(samples)
    sstat = stats_of(steady, effective_duration(steady))
    tl = timeline(samples, t0)
    lp = latency_pattern(tl)

    name = os.path.basename(path)
    lines.append("=" * 78)
    lines.append("文件: %s" % name)
    lines.append("=" * 78)

    # ---- 1. 概览
    lines.append("")
    lines.append("【1】概览")
    lines.append("  总样本        : %d" % overall["n"])
    lines.append("  墙上时间跨度  : %.1f 秒" % ((samples[-1]["ts"] + samples[-1]["el"] - t0) / 1000.0))
    lines.append("  有效运行时长  : %.1f 秒" % eff)
    if len(segs) > 1:
        lines.append("  ⚠️  检测到 %d 个运行片段 —— 这个 JTL 是多次运行追加拼接的（JMeter GUI 模式会 append）：" % len(segs))
        for i, (s, e) in enumerate(segs, 1):
            sub = [x for x in samples if s <= int(x["ts"] / 1000) <= e]
            lines.append("       片段%d: 第 %d~%d 秒，%d 个请求，约 %.0f TPS" %
                         (i, s - int(t0 / 1000), e - int(t0 / 1000), len(sub), len(sub) / max(1, e - s + 1)))
        lines.append("     TPS 已按有效时长计算（未把片段间的空档算进去）")
    lines.append("  整体 TPS      : %.1f" % overall["tps"])
    lines.append("  失败样本      : %d (%.3f%%)" % (overall["err"], overall["err_pct"]))
    lines.append("  最大并发线程  : %s" % (max_threads if max_threads else "JTL 无 allThreads 列"))
    lines.append("  平均并发请求  : %.2f   ← Little's Law = TPS × 响应时间" % overall["concurrency"])
    if warm:
        lines.append("  已剔除预热样本: %d 条（预热线程组的结果不参与统计）" % len(warm))
    if skipped:
        lines.append("  （跳过的异常行: %d）" % skipped)
    if overall["err"]:
        reasons = {}
        for s in samples:
            if not s["ok"]:
                key = s["msg"][:88] or s["code"][:88] or "(无信息)"
                reasons[key] = reasons.get(key, 0) + 1
        lines.append("")
        lines.append("  失败原因分布:")
        for k, v in sorted(reasons.items(), key=lambda kv: -kv[1])[:5]:
            lines.append("    %5d 次  %s" % (v, k))

    # ---- 2. 分请求
    lines.append("")
    lines.append("【2】分请求统计（整体）")
    lines.append("  %-42s %8s %8s %8s %8s %8s %8s %8s" %
                 ("label", "样本", "TPS", "平均", "P90", "P95", "P99", "错误%"))
    by_label = {}
    for s in samples:
        by_label.setdefault(s["label"], []).append(s)
    for label, group in sorted(by_label.items(), key=lambda kv: -len(kv[1])):
        st = stats_of(group, effective_duration(group))
        lines.append("  %-42s %8d %8.1f %8.1f %8.1f %8.1f %8.1f %7.2f%%" % (
            label[:42], st["n"], st["tps"], st["avg"],
            st["p90"], st["p95"], st["p99"], st["err_pct"]))

    # ---- 3. 稳态
    lines.append("")
    lines.append("【3】稳态区间（已剔除 ramp-up 爬坡段）")
    if sstat:
        lines.append("  区间          : 第 %.0f 秒 ~ 第 %.0f 秒（共 %.1f 秒）" %
                     ((steady[0]["ts"] - t0) / 1000.0, (steady[-1]["ts"] - t0) / 1000.0, sstat["span"]))
        lines.append("  稳态 TPS      : %.1f" % sstat["tps"])
        lines.append("  响应时间      : 平均 %.1f | P50 %.1f | P90 %.1f | P95 %.1f | P99 %.1f | 最大 %.1f (ms)" %
                     (sstat["avg"], sstat["p50"], sstat["p90"], sstat["p95"], sstat["p99"], sstat["max"]))
        lines.append("  稳态平均并发  : %.2f" % sstat["concurrency"])
        lines.append("")
        lines.append("  🚩 诊断: 稳态并发 %.2f / 线程数 %s" %
                     (sstat["concurrency"], max_threads if max_threads else "?"))
        if max_threads and sstat["concurrency"] < max_threads * 0.5:
            lines.append("     ⚠️  并发远低于线程数 → 线程大量时间在闲置，压力没有真正打满；")
            lines.append("        检查 ramp-up 是否过长、或响应太快导致线程无事可做。")
        else:
            lines.append("     ✅ 并发已接近线程数 → 线程处于等待响应状态，压力有效。")

    # ---- 4. 时间线
    lines.append("")
    lines.append("【4】每秒时间线")
    tps_unit = max(1.0, max((t["n"] for t in tl), default=1) / 40.0)
    lines.append("  秒    请求  TPS图（1格=%.0f req/s）" % tps_unit)
    for t in tl:
        lines.append("  %4d  %5d  %s" % (t["sec"], t["n"], bar(t["n"], tps_unit)))
    lines.append("")
    lines.append("  秒    平均RT  RT图（1格=%.1f ms）        该秒最大RT" % max(0.1, max((t["avg"] for t in tl), default=1) / 30.0))
    rt_unit = max(0.1, max((t["avg"] for t in tl), default=1) / 30.0)
    for t in tl:
        lines.append("  %4d  %6.1f  %-32s %8.1f" % (t["sec"], t["avg"], bar(t["avg"], rt_unit), t["max"]))

    # ---- 5. 延迟结构
    lines.append("")
    lines.append("【5】延迟结构分析：是「持续性锁竞争」还是「周期性落库阻塞」？")
    if not lp:
        lines.append("  样本不足以分析")
    else:
        lines.append("  分析片段      : 第 %d~%d 秒（最长连续片段，共 %d 秒）" % (lp["seg"][0], lp["seg"][1], lp["n"]))
        lines.append("  每秒最大 RT   : P50 %.0f | P90 %.0f | 最大 %.0f (ms)" % (lp["p50_max"], lp["p90_max"], lp["max_max"]))
        lines.append("  每秒平均 RT   : P50 %.1f (ms)" % lp["p50_avg"])
        ratio = lp["p50_max"] / max(0.1, lp["p50_avg"])
        if ratio > 2:
            lines.append("  A) 持续性尾部延迟: 每秒最大 RT 的中位数是每秒平均 RT 的 %.1f 倍" % ratio)
            lines.append("     → 每一秒都有请求被卡很久 = 临界区（锁内操作）长期处于饱和排队状态")
        else:
            lines.append("  A) 无持续性尾部延迟（每秒最大 RT 与平均 RT 同一量级）")
        lines.append("  自相关        : %s" % "  ".join("lag%d=%+.3f" % (k, v) for k, v in lp["acs"].items()))
        if lp["period"]:
            lines.append("  B) 检测到 %d 秒周期成分 ✅ —— 与「延迟落库」定时任务周期一致" % lp["period"])
            lines.append("     → saveInteract() 在 synchronized 锁内做批量落库，每 3 秒阻塞一次在线请求")
        else:
            lines.append("  B) 未检测到明显周期成分 → 阻塞不是周期性的，而是持续性的锁竞争")
        lines.append("")
        lines.append("  读法：A 存在 = 锁粒度问题（每个请求都要进临界区）；")
        lines.append("        B 存在 = 落库与在线请求没有解耦。两个都命中，就该拆锁 + 异步化。")

    # ---- 6. 参数分布
    tids = [quantize_url(s["url"]) for s in samples]
    tids = [t for t in tids if t is not None]
    if tids:
        uniq = sorted(set(tids))
        counts = {}
        for t in tids:
            counts[t] = counts.get(t, 0) + 1
        top = sorted(counts.items(), key=lambda kv: -kv[1])[:5]
        lines.append("")
        lines.append("【6】参数（tid）分布")
        lines.append("  不同 tid 数   : %d（范围 %d ~ %d）" % (len(uniq), uniq[0], uniq[-1]))
        lines.append("  最热的 5 个   : %s" % ", ".join("%d×%d" % (t, c) for t, c in top))
        lines.append("  预期均匀分布下每个 tid 约被访问 %.1f 次" % (len(tids) / len(uniq)))
    return lines, {"name": name, "overall": overall, "steady": sstat,
                   "spikes": (lp["period"] or 0) if lp else 0, "max_threads": max_threads}


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    out_file = None
    if "--out" in sys.argv:
        i = sys.argv.index("--out")
        if i + 1 < len(sys.argv):
            out_file = sys.argv[i + 1]
            args = [a for a in args if a != out_file]
    if not args:
        print(__doc__)
        return

    all_lines = []
    summaries = []
    for path in args:
        if not os.path.exists(path):
            all_lines.append("!! 文件不存在: %s\n" % path)
            continue
        lines, summary = report(path)
        all_lines.extend(lines)
        all_lines.append("")
        if summary:
            summaries.append(summary)

    # ---- 多场景对比
    if len(summaries) > 1:
        all_lines.append("=" * 78)
        all_lines.append("三种场景对比（用稳态数据，才是可比的）")
        all_lines.append("=" * 78)
        all_lines.append("  %-22s %9s %7s %7s %7s %7s %8s %8s %6s" %
                         ("文件", "稳态TPS", "平均", "P90", "P95", "P99", "平均并发", "线程数", "周期"))
        for s in summaries:
            st = s["steady"] or s["overall"]
            all_lines.append("  %-22s %9.1f %7.1f %7.1f %7.1f %7.1f %8.2f %8s %6d" % (
                s["name"][:22], st["tps"], st["avg"], st["p90"], st["p95"], st["p99"],
                st["concurrency"], s["max_threads"] or "?", s["spikes"]))
        all_lines.append("")
        all_lines.append("  读法：")
        all_lines.append("   - TPS 高 + 并发接近线程数 = 压上去了，瓶颈在服务端")
        all_lines.append("   - TPS 低 + 并发远小于线程数 = 压测端没打满，数据不可用")
        all_lines.append("   - P99 远大于平均 = 存在毛刺，看第【5】节")

    text = "\n".join(all_lines)
    print(text)
    if out_file:
        with open(out_file, "w", encoding="utf-8") as f:
            f.write(text + "\n")
        print("\n(已写入 %s)" % out_file)


if __name__ == "__main__":
    main()
