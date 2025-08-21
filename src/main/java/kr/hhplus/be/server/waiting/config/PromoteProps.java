package kr.hhplus.be.server.waiting.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue.promote")
public class PromoteProps {

    private int capacity = 10;

    private int maxBatch = 100;


    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getMaxBatch() { return maxBatch; }
    public void setMaxBatch(int maxBatch) { this.maxBatch = maxBatch; }
}
