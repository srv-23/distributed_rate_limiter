local current = tonumber(redis.call('GET', KEYS[1]) or '0')

local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

if current >= limit then
    return {0, current, 0}
end

local newCount = redis.call('INCR', KEYS[1])
if newCount == 1 then
    redis.call('EXPIRE', KEYS[1], window)
end

local remaining = limit - newCount

return {1, newCount, remaining}