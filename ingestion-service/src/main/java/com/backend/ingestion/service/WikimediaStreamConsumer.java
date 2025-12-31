package com.backend.ingestion.service;

import com.backend.ingestion.model.WikiEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class WikimediaStreamConsumer {

    private static final String WIKIMEDIA_SSE_URL = 
        "https://stream.wikimedia.org/v2/stream/recentchange";

    private final KafkaTemplate<String, WikiEvent> kafkaTemplate;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final EventEnrichmentService enrichmentService;
    private EventSource eventSource;
    private volatile boolean running = true;
    private int reconnectAttempts = 0;

    public WikimediaStreamConsumer(
            KafkaTemplate<String, WikiEvent> kafkaTemplate,
            OkHttpClient httpClient,
            ObjectMapper objectMapper,
            EventEnrichmentService enrichmentService) {
        this.kafkaTemplate = kafkaTemplate;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.enrichmentService = enrichmentService;
    }

    @PostConstruct
    public void startConsuming() {
        log.info("Starting Wikimedia SSE stream consumer...");
        connect();
    }

    private void connect() {
        Request request = new Request.Builder()
                .url(WIKIMEDIA_SSE_URL)
                .header("Accept", "text/event-stream")
                .header("User-Agent", "WikiKafkaProject/1.0 (Educational Project; contact@example.com)")
                .build();

        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                log.info("Connected to Wikimedia SSE stream");
                reconnectAttempts = 0;
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                if (!running) {
                    return;
                }

                try {
                    WikiEvent event = parseEvent(data);
                    WikiEvent enrichedEvent = enrichmentService.enrich(event);
                    publishToKafka(enrichedEvent);
                } catch (Exception e) {
                    log.error("Error processing event: {}", data, e);
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                log.warn("SSE connection closed");
                if (running) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                String responseInfo = response != null ? "HTTP " + response.code() : "No response";
                String errorMsg = t != null ? t.getMessage() : "Unknown error";
                String errorClass = t != null ? t.getClass().getName() : "N/A";
                log.error("SSE connection failed: {} - {} ({})", responseInfo, errorMsg, errorClass);
                if (running) {
                    scheduleReconnect();
                }
            }
        };

        this.eventSource = EventSources.createFactory(httpClient)
                .newEventSource(request, listener);
    }

    private void scheduleReconnect() {
        long delay = Math.min(60_000, 1000L * (1L << Math.min(6, reconnectAttempts++)));
        log.info("Scheduling reconnect in {} ms", delay);
        
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                if (running) {
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private WikiEvent parseEvent(String data) throws Exception {
        return objectMapper.readValue(data, WikiEvent.class);
    }

    private void publishToKafka(WikiEvent event) {
        String key = event.getServerName() != null ? event.getServerName() : "unknown";
        
        CompletableFuture<?> future = kafkaTemplate.send("wiki_changes", key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to Kafka: key={}, eventId={}", 
                         key, event.getId(), ex);
            } else {
                log.debug("Sent to Kafka: key={}, eventId={}", key, event.getId());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Wikimedia stream consumer...");
        running = false;
        if (eventSource != null) {
            eventSource.cancel();
        }
    }
}