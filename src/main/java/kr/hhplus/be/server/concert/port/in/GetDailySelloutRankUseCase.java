package kr.hhplus.be.server.concert.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GetDailySelloutRankUseCase {

    Result get(String yyyymmdd);

    record Result(String date, List<Row> rows) {
        public record Row(Long concertId, Long concertDateId, long selloutSeconds,
                          String title, LocalDate concertDate) {}
    }
}