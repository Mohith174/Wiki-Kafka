package com.backend.consumer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class SlowConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong messageCount = new AtomicLong(0);

    @KafkaListener(
        topics = "wiki_aggregates",
        groupId = "slow",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            long count = messageCount.incrementAndGet();
            JsonNode node = objectMapper.readTree(message);
            
            String wiki = node.has("wiki") ? node.get("wiki").asText() : "unknown";
            long eventCount = node.has("count") ? node.get("count").asLong() : 0;
            
            // Simulate slow processing
            Thread.sleep(100);
            
            log.debug("Slow consumer #{}: wiki={}, count={}", count, wiki, eventCount);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    public long getMessageCount() {
        return messageCount.get();
    }
}