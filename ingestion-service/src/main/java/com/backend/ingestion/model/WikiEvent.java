package com.backend.ingestion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiEvent {
    private String id;
    private String type;
    private Integer namespace;
    private String title;
    private String comment;
    private Long timestamp;
    private String user;
    private Boolean bot;
    private String serverUrl;
    private String serverName;
    private String wiki;
    private String region;
    private String language;
    private Long processingTimestamp;
}