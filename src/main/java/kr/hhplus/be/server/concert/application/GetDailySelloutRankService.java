package kr.hhplus.be.server.concert.application;

import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.port.in.GetDailySelloutRankUseCase;
import kr.hhplus.be.server.concert.port.out.ConcertRepository;
import kr.hhplus.be.server.concert.port.out.SelloutDailyRankReader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class GetDailySelloutRankService implements GetDailySelloutRankUseCase {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");;
    private final SelloutDailyRankReader rankReader;
    private final ConcertRepository concertRepository;


    @Override
    @Transactional(readOnly = true)
    public Result get(String yyyymmdd) {

        LocalDate date = LocalDate.parse(yyyymmdd, YYYYMMDD);

        var entries = rankReader.top20(date);
        var rows = new ArrayList<Result.Row>(entries.size());

        for (var e : entries) {
            String member = e.getKey();             
            long seconds = (long) Math.abs(e.getValue());

            String[] parts = member.split(":");
            Long concertId = Long.valueOf(parts[0]);
            Long concertDateId = Long.valueOf(parts[1]);

            ConcertDate cd = concertRepository.findConcertDateById(concertDateId).orElse(null);
            String title = cd != null ? cd.getConcert().getTitle() : null;
            var cdate = cd != null ? cd.getConcertDate() : null;

            rows.add(new Result.Row(concertId, concertDateId, seconds, title, cdate));
        }
        return new Result(yyyymmdd, rows);
    }
}