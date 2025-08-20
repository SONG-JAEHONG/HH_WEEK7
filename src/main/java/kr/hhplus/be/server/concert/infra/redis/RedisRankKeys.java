package kr.hhplus.be.server.concert.infra.redis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class RedisRankKeys {

    private RedisRankKeys() {}

    public static String member(Long concertId, Long concertDateId) {
        return concertId + ":" + concertDateId;
    }

    public static String ratioAll() { return "rank:ratio:all"; }

    public static String fastAll() { return "rank:fast:all"; }

    public static String compositeCache() { return "rank:pop:composite:cache"; }
}
