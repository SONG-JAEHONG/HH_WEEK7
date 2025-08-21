package kr.hhplus.be.server.waiting.application;

import kr.hhplus.be.server.waiting.infra.redis.WaitingLuaScripts;
import kr.hhplus.be.server.waiting.infra.web.dto.IssueTicketResponse;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueIssueUseCase;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaitingQueueIssueService implements WaitingQueueIssueUseCase {

    private final RedissonClient redissonClient;

    private static final long SCORE_FACTOR   = 1_000_000L;
    private static final int  SEQ_TTL_SEC    = 5;

    @Override
    public IssueTicketResponse issueTicket(Long userId) {
        String token = UUID.randomUUID().toString();

        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
        @SuppressWarnings("unchecked")
        List<Object> res = (List<Object>) rScript.eval(
                RScript.Mode.READ_WRITE,
                WaitingLuaScripts.ISSUE_TICKET,
                RScript.ReturnType.MULTI,
                Collections.emptyList(),
                String.valueOf(userId),
                token,
                String.valueOf(SCORE_FACTOR),
                String.valueOf(SEQ_TTL_SEC)
        );

        String finalToken = String.valueOf(res.get(1));
        long rank = Long.parseLong(String.valueOf(res.get(2)));

        return new IssueTicketResponse(finalToken, rank);
    }
}
