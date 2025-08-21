package kr.hhplus.be.server.waiting.application;

import kr.hhplus.be.server.waiting.infra.redis.WaitingCompleteLuaScripts;
import kr.hhplus.be.server.waiting.infra.redis.WaitingQueueKeys;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueCompleteUseCase;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitingQueueCompleteService implements WaitingQueueCompleteUseCase {

    private final RedissonClient redisson;

    @Override
    public CompleteResult complete(String token) {
        RScript r = redisson.getScript(StringCodec.INSTANCE);

        @SuppressWarnings("unchecked")
        List<Object> res = (List<Object>) r.eval(
                RScript.Mode.READ_WRITE,
                WaitingCompleteLuaScripts.COMPLETE_AND_CLEANUP,
                RScript.ReturnType.MULTI,
                Arrays.asList(WaitingQueueKeys.workingZ(), WaitingQueueKeys.activeTokenH()),
                token
        );

        String status = String.valueOf(res.get(0));
        if (!"OK".equals(status)) {

            throw new IllegalStateException("Ticket not found for token=" + token);
        }
        String userId = String.valueOf(res.get(1));
        boolean removed = "1".equals(String.valueOf(res.get(2)));
        return new CompleteResult(userId, removed);
    }
}
