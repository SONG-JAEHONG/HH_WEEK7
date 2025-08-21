package kr.hhplus.be.server.waiting.infra.redis;

public final class WaitingLuaScripts {

    private WaitingLuaScripts(){}

    public static final String ISSUE_TICKET = """
local userId     = ARGV[1]
local token      = ARGV[2]
local FACTOR     = tonumber(ARGV[3])
local seqTtlSec  = tonumber(ARGV[4])

local zWaiting   = 'z:{queue}:waiting'
local hActive    = 'h:{queue}:active_token'
local hTicket    = 'h:{queue}:ticket:' .. token

-- 1) 중복 검사
local exist = redis.call('HGET', hActive, userId)
if exist then
  local r = redis.call('ZRANK', zWaiting, exist)
  if r then r = r + 1 else r = 0 end
  return { 'EXISTS', exist, r }
end

-- 2) 사용자→토큰 점유 (경합)
local claimed = redis.call('HSETNX', hActive, userId, token)
if claimed == 0 then
  local t = redis.call('HGET', hActive, userId)
  local r = redis.call('ZRANK', zWaiting, t)
  if r then r = r + 1 else r = 0 end
  return { 'EXISTS', t, r }
end

-- 3) 서버 시간(ms)
local t  = redis.call('TIME')       -- {sec, usec}
local ms = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

-- 4) per-ms INCR
local seqKey = 'seq:{queue}:waiting:' .. ms
local seq    = redis.call('INCR', seqKey)
redis.call('EXPIRE', seqKey, seqTtlSec)

-- 5) score = ms * FACTOR + seq
local score = ms * FACTOR + seq

-- 6) 대기열 삽입
redis.call('ZADD', zWaiting, score, token)

-- 7) 티켓 메타 저장
redis.call('HSET', 'h:{queue}:ticket:' .. token,
  'userId', userId,
  'state', 'waiting',
  'issuedAt', ms,
  'enqueuedAt', score
)

-- 8) 순번 계산(1-base)
local rank = redis.call('ZRANK', zWaiting, token)
if rank then
  rank = rank + 1
else
  rank = 0
end

return { 'OK', token, rank }
""";
}
