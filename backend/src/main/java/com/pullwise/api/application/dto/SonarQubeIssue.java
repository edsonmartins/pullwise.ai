package com.pullwise.api.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO para issues do SonarQube.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarQubeIssue {

    private String key;
    private String rule;
    private String severity;
    private String type;
    private String message;

    @JsonProperty("creationDate")
    private String creationDate;

    @JsonProperty("updateDate")
    private String updateDate;

    private String status;
    private String resolution;

    @JsonProperty("author")
    private String author;

    @JsonProperty("component")
    private String component;

    @JsonProperty("project")
    private String project;

    private String file;
    private String line;

    @JsonProperty("textRange")
    private TextRange textRange;

    @JsonProperty("debt")
    private Debt debt;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextRange {
        private Integer startLine;
        private Integer endLine;
        private Integer startOffset;
        private Integer endOffset;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Debt {
        private String value;
    }
}
