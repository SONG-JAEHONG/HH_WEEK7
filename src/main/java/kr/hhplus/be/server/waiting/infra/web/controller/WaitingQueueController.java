package kr.hhplus.be.server.waiting.infra.web.controller;


import kr.hhplus.be.server.waiting.application.WaitingQueueIssueService;
import kr.hhplus.be.server.waiting.infra.web.dto.IssueTicketRequest;
import kr.hhplus.be.server.waiting.infra.web.dto.IssueTicketResponse;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueIssueUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/waiting")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueIssueUseCase waitingQueueIssueUseCase;

    @PostMapping("/issueToken")
    public IssueTicketResponse issue (@RequestBody IssueTicketRequest req){
        if(req.userId() == null || req.userId() <= 0){
            throw new IllegalArgumentException("아이디가 필요합니다");
        }
        return waitingQueueIssueUseCase.issueTicket(req.userId());
    }

}
