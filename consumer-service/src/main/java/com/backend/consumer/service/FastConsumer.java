package com.backend.consumer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class FastConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong messageCount = new AtomicLong(0);

    @KafkaListener(
        topics = "wiki_aggregates",
        groupId = "fast",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            long count = messageCount.incrementAndGet();
            JsonNode node = objectMapper.readTree(message);
            
            String wiki = node.has("wiki") ? node.get("wiki").asText() : "unknown";
            long eventCount = node.has("count") ? node.get("count").asLong() : 0;
            
            log.debug("Fast consumer #{}: wiki={}, count={}", count, wiki, eventCount);
            
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    public long getMessageCount() {
        return messageCount.get();
    }
}