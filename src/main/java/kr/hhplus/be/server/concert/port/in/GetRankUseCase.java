package kr.hhplus.be.server.concert.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GetRankUseCase {
    Result top(int limit);

    record Result(List<Row> rows) {
        public record Row(Long concertId, Long concertDateId, double score, String title, LocalDate concertDate) {}
    }

}
