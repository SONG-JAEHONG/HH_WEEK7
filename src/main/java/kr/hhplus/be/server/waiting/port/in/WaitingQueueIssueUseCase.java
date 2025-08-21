package kr.hhplus.be.server.waiting.port.in;

import kr.hhplus.be.server.waiting.infra.web.dto.IssueTicketResponse;

public interface WaitingQueueIssueUseCase {

    IssueTicketResponse issueTicket(Long userId) ;

}
