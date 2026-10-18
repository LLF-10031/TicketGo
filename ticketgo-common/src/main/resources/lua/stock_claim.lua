-- 通用领取引擎：原子扣减库存 + 用户维度限购
-- KEYS[1] = 库存 key
-- KEYS[2] = 用户已领/已购 Hash key
-- ARGV[1] = userId
-- ARGV[2] = 每用户限购数（票秒杀=1；券=模板配置）
-- 返回：1 成功 / 0 库存不足 / -1 库存未预热 / -2 超出限购
if redis.call('exists', KEYS[1]) == 0 then
    return -1
end
local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil or stock <= 0 then
    return 0
end
local used = tonumber(redis.call('hget', KEYS[2], ARGV[1]) or '0')
if used >= tonumber(ARGV[2]) then
    return -2
end
redis.call('decr', KEYS[1])
redis.call('hincrby', KEYS[2], ARGV[1], 1)
return 1
