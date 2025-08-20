package kr.hhplus.be.server.redis;

import kr.hhplus.be.server.concert.infra.redis.RedisKeys;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertRequest;
import kr.hhplus.be.server.concert.port.in.ConcertCommandUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles("test")
class Step1_ConcertRegistrationCreatesRedisKeysIT {

    @Autowired ConcertCommandUseCase concertCommandUseCase;
    @Autowired StringRedisTemplate redis;

    @AfterEach
    void clean() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void 콘서트_등록_남은좌석_및_전체좌석_레디스_키_생성() {
        int totalSeats = 150;
        LocalDate date = LocalDate.of(2025, 9, 1);
        LocalDateTime openAt = LocalDateTime.of(2025, 8, 20, 9, 0, 0);

        var req = new CreateConcertRequest(
                "테스트콘서트",
                List.of(new CreateConcertRequest.Date(date, openAt, totalSeats))
        );

        var res = concertCommandUseCase.createConcert(req);
        Long concertDateId = res.concertDateIds().get(0);

        String totalKey  = RedisKeys.TotalSeat(concertDateId);
        String remainKey = RedisKeys.RemainSeat(concertDateId);


        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(redis.opsForValue().get(totalKey)).isEqualTo("150");
            assertThat(redis.opsForValue().get(remainKey)).isEqualTo("150");
        });

    }
}
