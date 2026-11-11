-- Read current time from Redis (milliseconds).
local time = redis.call('TIME')
local now = (tonumber(time[1]) * 1000)
          + math.floor(tonumber(time[2]) / 1000)

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local requestCost = tonumber(ARGV[3])

-- Read existing bucket state.
local tokens = tonumber(
    redis.call('HGET', KEYS[1], 'tokens')
)

local lastRefill = tonumber(
    redis.call('HGET', KEYS[1], 'lastRefill')
)

-- Initialize a new bucket as full.
if tokens == nil then
    tokens = capacity
    lastRefill = now
end

-- Calculate elapsed time.
local elapsed = math.max(0, now - lastRefill)

-- Refill tokens.
local newTokens = (elapsed * refillRate) / 1000

tokens = math.min(capacity, tokens + newTokens)

-- Update the last refill timestamp.
lastRefill = now

local allowed = 0

-- Consume tokens if enough are available.
if tokens >= requestCost then
    tokens = tokens - requestCost
    allowed = 1
end

-- Save updated bucket state.
redis.call(
    'HSET',
    KEYS[1],
    'tokens', string.format('%.6f', tokens),
    'lastRefill', lastRefill
)

-- Expire the key after enough time to refill a full bucket.
local ttl = math.ceil(capacity / refillRate) + 1
redis.call('EXPIRE', KEYS[1], ttl)

return {
    tostring(allowed),
    string.format('%.6f', tokens)
    }
