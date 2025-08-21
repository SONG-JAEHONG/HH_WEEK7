package kr.hhplus.be.server.waiting.application;

import kr.hhplus.be.server.waiting.infra.redis.WaitingPromoteLuaScripts;
import kr.hhplus.be.server.waiting.infra.redis.WaitingQueueKeys;
import kr.hhplus.be.server.waiting.port.in.WaitingQueuePromoteUseCase;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WaitingQueuePromoteService implements WaitingQueuePromoteUseCase {

    private final RedissonClient redissonClient;


    private final int capacity = 10;
    private final int maxBatch = 50;


    @Override
    public PromoteResult promoteOnce() {
        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);

        @SuppressWarnings("unchecked")
        List<Object> res = (List<Object>) rScript.eval(
                RScript.Mode.READ_WRITE,
                WaitingPromoteLuaScripts.PROMOTE_TO_WORKING,
                RScript.ReturnType.MULTI,
                Arrays.asList(WaitingQueueKeys.waitingZ(), WaitingQueueKeys.workingZ()),
                String.valueOf(capacity),
                String.valueOf(maxBatch)
        );

        long moved = Long.parseLong(String.valueOf(res.get(0)));
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i < res.size(); i++) {
            tokens.add(String.valueOf(res.get(i)));
        }
        return new PromoteResult(moved, tokens);
    }


}