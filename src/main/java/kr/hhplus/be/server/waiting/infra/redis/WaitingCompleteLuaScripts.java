package kr.hhplus.be.server.waiting.infra.redis;

public final class WaitingCompleteLuaScripts {
    private WaitingCompleteLuaScripts() {}

    public static final String COMPLETE_AND_CLEANUP = """
local zWorking = KEYS[1]
local hActive  = KEYS[2]
local token    = ARGV[1]
local hTicket  = 'h:{queue}:ticket:' .. token

-- 티켓 존재 및 사용자 ID 조회
local uid = redis.call('HGET', hTicket, 'userId')
if (not uid) then
  return {'NOT_FOUND'}  -- 티켓 자체가 없으면 종료
end

-- 서버 시간(ms)
local t  = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

-- working에서 제거 (멱등: 이미 없으면 0)
local removed = redis.call('ZREM', zWorking, token)

-- 상태를 done으로 기록 (멱등: 재호출 시 덮어쓰기)
redis.call('HSET', hTicket, 'state', 'done', 'completedAt', now)

-- 사용자→토큰 매핑 제거 (멱등)
redis.call('HDEL', hActive, uid)

-- 결과: 상태, userId, zrem 결과
return {'OK', uid, removed}
""";
}
