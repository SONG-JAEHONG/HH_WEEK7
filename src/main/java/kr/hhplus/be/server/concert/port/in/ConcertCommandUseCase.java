package kr.hhplus.be.server.concert.port.in;

import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertRequest;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertResponse;
public interface ConcertCommandUseCase {
    CreateConcertResponse createConcert(CreateConcertRequest request);
}
