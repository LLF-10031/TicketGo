-- 滑动窗口限流：清理过期 -> 计数 -> 写入
-- KEYS[1]=limiter key  ARGV[1]=now(ms)  ARGV[2]=window(ms)  ARGV[3]=limit
redis.call('zremrangebyscore', KEYS[1], 0, ARGV[1] - ARGV[2])
local count = redis.call('zcard', KEYS[1])
if count >= tonumber(ARGV[3]) then
    return 0
end
redis.call('zadd', KEYS[1], ARGV[1], ARGV[1])
redis.call('pexpire', KEYS[1], ARGV[2])
return 1
