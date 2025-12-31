package com.backend.ingestion.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProducerMetrics {

    private final Counter eventsReceived;
    private final Counter eventsPublished;
    private final Counter eventsFailed;

    public ProducerMetrics(MeterRegistry meterRegistry) {
        this.eventsReceived = Counter.builder("wiki.events.received")
                .description("Total events received from Wikimedia")
                .register(meterRegistry);
        this.eventsPublished = Counter.builder("wiki.events.published")
                .description("Total events published to Kafka")
                .register(meterRegistry);
        this.eventsFailed = Counter.builder("wiki.events.failed")
                .description("Total events failed to publish")
                .register(meterRegistry);
    }

    public void recordEventReceived() {
        eventsReceived.increment();
    }

    public void recordEventPublished() {
        eventsPublished.increment();
    }

    public void recordEventFailed() {
        eventsFailed.increment();
    }
}