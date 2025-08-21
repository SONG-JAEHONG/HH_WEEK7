package kr.hhplus.be.server.concert.application;

import kr.hhplus.be.server.concert.port.in.GetRankUseCase;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.infra.redis.RedisRankKeys;
import kr.hhplus.be.server.concert.port.in.GetRankUseCase;
import kr.hhplus.be.server.concert.port.out.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@Service
@RequiredArgsConstructor
public class GetRankService implements GetRankUseCase{

    private final StringRedisTemplate redis;
    private final ConcertRepository concertRepository;

    @Override
    @Transactional(readOnly = true)
    public Result top(int limit) {

        String key = RedisRankKeys.compositeCache();
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(key, 0, Math.max(0, limit - 1));

        List<Result.Row> rows = new ArrayList<>();
        if (tuples == null) return new Result(rows);

        for (var t : tuples) {
            if (t.getValue() == null || t.getScore() == null) continue;
            String member = t.getValue();
            double score = t.getScore();
            String[] parts = member.split(":");
            if (parts.length != 2) continue;

            Long concertId = Long.valueOf(parts[0]);
            Long concertDateId = Long.valueOf(parts[1]);

            ConcertDate cd = concertRepository.findConcertDateByIdOrThrow(concertDateId);
            String title = (cd != null && cd.getConcert() != null) ? cd.getConcert().getTitle() : null;
            var date = (cd != null) ? cd.getConcertDate() : null;

            rows.add(new Result.Row(concertId, concertDateId, score, title, date));
        }
        return new Result(rows);
    }
}
