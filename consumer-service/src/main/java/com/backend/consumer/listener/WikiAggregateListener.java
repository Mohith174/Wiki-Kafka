package com.backend.consumer.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class WikiAggregateListener {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong messageCount = new AtomicLong(0);

    @KafkaListener(topics = "wiki_aggregates", groupId = "wiki-consumer-group")
    public void consume(String message) {
        try {
            long count = messageCount.incrementAndGet();
            JsonNode node = objectMapper.readTree(message);
            
            String wiki = node.has("wiki") ? node.get("wiki").asText() : "unknown";
            long eventCount = node.has("count") ? node.get("count").asLong() : 0;
            
            log.info("Received aggregate #{}: wiki={}, count={}", count, wiki, eventCount);
            
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    public long getMessageCount() {
        return messageCount.get();
    }
}