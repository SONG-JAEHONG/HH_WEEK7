package kr.hhplus.be.server.concert.infra.redis;


public final class RedisKeys {
    private RedisKeys() {}
    public static String RemainSeat(long concertDateId) {
        return "seats:remain:" + concertDateId;
    }
}
