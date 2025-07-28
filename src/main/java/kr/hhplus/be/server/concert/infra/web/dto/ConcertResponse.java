package kr.hhplus.be.server.concert.infra.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConcertResponse {

    private Long id;
    private String title;

}
