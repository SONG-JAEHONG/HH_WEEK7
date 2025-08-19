package kr.hhplus.be.server.concert.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SelloutDailyRankReader {
    List<Map.Entry<String, Double>> top20(LocalDate date);
}
