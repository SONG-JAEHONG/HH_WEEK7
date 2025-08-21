package kr.hhplus.be.server.waiting.infra.redis;

public final class WaitingPromoteLuaScripts {

    private WaitingPromoteLuaScripts(){}

    public static final String PROMOTE_TO_WORKING = """
local zWaiting  = KEYS[1]
local zWorking  = KEYS[2]
local capacity  = tonumber(ARGV[1])
local maxBatch  = tonumber(ARGV[2])

-- 현재 working 인원
local current = redis.call('ZCARD', zWorking)
local free = capacity - current
if free <= 0 then
  return {0}
end

-- 이번 라운드에서 실제로 승격할 수 있는 최대치
local quota = free
if maxBatch and maxBatch > 0 and maxBatch < quota then
  quota = maxBatch
end

-- 서버 시간(ms)
local t  = redis.call('TIME')      -- {sec, usec}
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local moved = 0
local promoted = {0}  -- 첫 칸에 moved 카운트를 넣을 예정

while quota > 0 do
  local pair = redis.call('ZPOPMIN', zWaiting, 1)
  if (not pair) or (#pair == 0) then
    break
  end
  local token = pair[1]
  -- working에 추가
  redis.call('ZADD', zWorking, now, token)
  redis.call('HSET', 'h:{queue}:ticket:' .. token,
             'state', 'working', 'admittedAt', now)
  moved = moved + 1
  table.insert(promoted, token)
  quota = quota - 1
end

promoted[1] = moved
return promoted
""";
}
