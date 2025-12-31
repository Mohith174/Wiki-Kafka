package com.backend.processor.topology;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class WikiAggregationTopology {

    private static final String INPUT_TOPIC = "wiki_changes";
    private static final String OUTPUT_TOPIC = "wiki_aggregates";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        log.info("Building Wiki stream topology with String Serdes...");

        // Read as String, not as WikiEvent
        KStream<String, String> inputStream = streamsBuilder.stream(
            INPUT_TOPIC,
            Consumed.with(Serdes.String(), Serdes.String())
        );

        // Extract serverName from JSON string for grouping
        KStream<String, String> keyedStream = inputStream
            .peek((key, value) -> log.debug("Received event with key: {}", key))
            .map((key, value) -> {
                String serverName = extractServerName(value);
                return KeyValue.pair(serverName, value);
            });

        // Count events per server in 1-minute windows
        KTable<Windowed<String>, Long> aggregated = keyedStream
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .count(Materialized.as("wiki-counts-store"));

        // Output aggregated results as JSON strings
        aggregated.toStream()
            .map((windowedKey, count) -> {
                String wiki = windowedKey.key();
                long windowStart = windowedKey.window().start();
                long windowEnd = windowedKey.window().end();

                String jsonOutput = String.format(
                    "{\"wiki\":\"%s\",\"count\":%d,\"windowStart\":%d,\"windowEnd\":%d}",
                    wiki, count, windowStart, windowEnd
                );

                log.info("Aggregate: {} -> count={}", wiki, count);
                return KeyValue.pair(wiki, jsonOutput);
            })
            .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        log.info("Wiki stream topology built successfully");
    }

    private String extractServerName(String jsonValue) {
        try {
            JsonNode node = objectMapper.readTree(jsonValue);
            if (node.has("serverName") && !node.get("serverName").isNull()) {
                return node.get("serverName").asText();
            }
            if (node.has("server_name") && !node.get("server_name").isNull()) {
                return node.get("server_name").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to parse serverName from JSON: {}", e.getMessage());
        }
        return "unknown";
    }
}