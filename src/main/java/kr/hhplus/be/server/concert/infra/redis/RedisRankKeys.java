package kr.hhplus.be.server.concert.infra.redis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class RedisRankKeys {

    private static final DateTimeFormatter DAILY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final WeekFields ISO = WeekFields.ISO;

    private RedisRankKeys() {}

    public static String daily(LocalDateTime selloutAt) {
        return "rank:sellout:daily:" + DAILY_FMT.format(selloutAt);
    }

    public static String daily(LocalDate date) {
        return "rank:sellout:daily:" + DAILY_FMT.format(date);
    }

    public static String member(Long concertId, Long concertDateId) {
        return concertId + ":" + concertDateId;
    }

}
