package com.example.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 限流通用工具
 * 针对于不同的情况进行限流操作，支持限流升级
 */
@Slf4j
@Component
public class FlowUtils {

    @Resource
    StringRedisTemplate template;

    private static final LimitAction defaultAction = overclock -> !overclock;

    /**
     * 周期限流脚本：把「查是否已封禁 → 计数 → 超限则封禁」三步合成一次原子操作。
     * 返回 1 放行，0 拒绝。
     */
    private static final DefaultRedisScript<Long> PERIOD_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            -- KEYS[1]=计数键  KEYS[2]=封禁键
            -- ARGV[1]=周期内最大次数  ARGV[2]=计数周期(秒)窗口  ARGV[3]=封禁时长(秒)
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return 0
            end
            local n = redis.call('INCR', KEYS[1])
            if n == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            if n > tonumber(ARGV[1]) then
                redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])
                return 0
            end
            return 1
            """, Long.class);

    /**
     * 与 {@link #limitPeriodCheck} 语义相同，但整个判定过程在 Redis 侧用 Lua 原子完成。
     * <p>
     * 相比 limitPeriodCheck 的两个优势：
     * 1) 网络往返从 3 次（hasKey + get + incr）降为 1 次；
     * 2) 调用方不再需要在 Java 侧用 synchronized 串行化，高并发下不会成为单点瓶颈；
     * 而且多实例部署时计数依然准确（单机锁在多实例下本来就会失真）。
     */
    public boolean limitPeriodAtomicCheck(String counterKey, String blockKey,
                                          int blockTime, int frequency, int period) {
        Long result = template.execute(PERIOD_LIMIT_SCRIPT,
                List.of(counterKey, blockKey),
                String.valueOf(frequency), String.valueOf(period), String.valueOf(blockTime));
        return result == 1L;
    }
    /**
     * 针对于单次频率限制，请求成功后，在冷却时间内不得再次进行请求，如3秒内不能再次发起请求
     * @param key 键
     * @param blockTime 限制时间
     * @return 是否通过限流检查
     */
    public boolean limitOnceCheck(String key, int blockTime){
        return this.internalCheck(key, 1, blockTime, defaultAction);
    }

    /**
     * 针对于单次频率限制，请求成功后，在冷却时间内不得再次进行请求
     * 如3秒内不能再次发起请求，如果不听劝阻继续发起请求，将限制更长时间
     * @param key 键
     * @param frequency 请求频率
     * @param baseTime 基础限制时间
     * @param upgradeTime 升级限制时间
     * @return 是否通过限流检查
     */
    public boolean limitOnceUpgradeCheck(String key, int frequency, int baseTime, int upgradeTime){
        return this.internalCheck(key, frequency, baseTime, (overclock) -> {
                    if (overclock)
                        template.opsForValue().set(key, "1", upgradeTime, TimeUnit.SECONDS);
            //没超频就放行；超频了才拦，并把计数重置成更长的封禁时间（阶梯封禁）
            return !overclock;
                });
    }

    /**
     * 针对于在时间段内多次请求限制，如(period)3秒内限制请求20次，超出频率则封禁一段时间
     * @param counterKey 计数键
     * @param blockKey 封禁键
     * @param blockTime 封禁时间
     * @param frequency 请求频率
     * @param period 计数周期
     * @return 是否通过限流检查
     */
    public boolean limitPeriodCheck(String counterKey, String blockKey, int blockTime, int frequency, int period){
        return this.internalCheck(counterKey, frequency, period, (overclock) -> {
                    if (overclock)
                        template.opsForValue().set(blockKey, "", blockTime, TimeUnit.SECONDS);
                    return !overclock;
                });
    }

    /**
     * 针对于多次请求限制，如3秒内20次请求
     * @param counterKey 计数键
     * @param frequency 请求频率
     * @param period 计数周期
     * @return 是否通过限流检查
     */
    public boolean limitPeriodCounterCheck(String counterKey,  int frequency, int period){
        return this.internalCheck(counterKey,frequency,period,defaultAction);
    }
    /**
     * 内部使用请求限制主要逻辑
     * @param key 计数键
     * @param frequency 请求频率
     * @param period 计数周期
     * @param action 限制行为与策略
     * @return 是否通过限流检查
     */
    private boolean internalCheck(String key, int frequency, int period, LimitAction action){
        String count = template.opsForValue().get(key);
        if (count != null) {
            long value = Optional.ofNullable(template.opsForValue().increment(key)).orElse(0L);
            int c = Integer.parseInt(count);
            if(value != c + 1)
                template.expire(key, period, TimeUnit.SECONDS);
            return action.run(value > frequency);
        } else {
            template.opsForValue().set(key, "1", period, TimeUnit.SECONDS);
            return true;
        }
    }

    /**
     * 限制行为与策略
     */
    private interface LimitAction {
        boolean run(boolean overclock);
    }
}
