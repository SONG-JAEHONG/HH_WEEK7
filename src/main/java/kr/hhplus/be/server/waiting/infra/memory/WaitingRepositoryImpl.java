package kr.hhplus.be.server.waiting.infra.memory;

import kr.hhplus.be.server.waiting.port.out.WaitingRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class WaitingRepositoryImpl implements WaitingRepository {

    private final Map<String, LocalDateTime> waiting = new HashMap<>();
    private final Queue<String> working = new LinkedList<>();

    private final int MAX_WORKING_SIZE = 10;

    @Override
    public void enterWaiting(String userId) {
        waiting.put(userId, LocalDateTime.now());
        fillWorking();
    }

    @Override
    public void fillWorking() {
        if(working.size() >= MAX_WORKING_SIZE) return;

        List<Map.Entry<String, LocalDateTime>> sortedWaiting = waiting.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        for (Map.Entry<String, LocalDateTime> entry : sortedWaiting) {
            if (working.size() >= MAX_WORKING_SIZE) break;

            String userId = entry.getKey();
            working.add(userId);
            waiting.remove(userId);
        }
    }

    @Override
    public int getPosition(String userId) {
        List<Map.Entry<String, LocalDateTime>> sortedWaiting = waiting.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        for (int i = 0; i < sortedWaiting.size(); i++) {
            if (sortedWaiting.get(i).getKey().equals(userId)) {
                return i + 1;
            }
        }

        return 0 ;
    }
}
