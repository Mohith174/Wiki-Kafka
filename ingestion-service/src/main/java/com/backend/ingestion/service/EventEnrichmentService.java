package com.backend.ingestion.service;

import com.backend.ingestion.model.WikiEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventEnrichmentService {

    public WikiEvent enrich(WikiEvent event) {
        event.setProcessingTimestamp(System.currentTimeMillis());

        if (event.getServerName() != null && event.getServerName().contains(".")) {
            String[] parts = event.getServerName().split("\\.");
            if (parts.length > 0) {
                event.setLanguage(parts[0]);
            }
        }

        if (event.getServerName() != null) {
            if (event.getServerName().contains("wikipedia.org")) {
                event.setRegion("global");
            } else {
                event.setRegion("local");
            }
        }

        return event;
    }
}