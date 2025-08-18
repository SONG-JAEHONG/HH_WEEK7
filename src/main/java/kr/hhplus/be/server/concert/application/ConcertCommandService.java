package kr.hhplus.be.server.concert.application;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.net.SyslogOutputStream;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.infra.event.ConcertDateCreateEvent;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertRequest;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertResponse;
import kr.hhplus.be.server.concert.port.in.ConcertCommandUseCase;
import kr.hhplus.be.server.concert.port.out.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ConcertCommandService implements ConcertCommandUseCase {

    private final ConcertRepository concertRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    public CreateConcertResponse createConcert(CreateConcertRequest request) {
        Concert concert = concertRepository.save(new Concert(request.title()));

        List<Long> dateIds = new ArrayList<>(request.dates().size());
        for (var d : request.dates()) {
            ConcertDate cd = concertRepository.save(
                    new ConcertDate(concert, d.concertDate(), d.openAt(), d.totalSeats())
            );
            dateIds.add(cd.getId());
            publisher.publishEvent(new ConcertDateCreateEvent(concert.getId(), d.totalSeats()));
        }

        return new CreateConcertResponse(concert.getId(), dateIds);
    }
}