package com.pullwise.api.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO para resposta da API do SonarQube.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarQubeResponse {

    private List<Issue> issues;
    private Paging paging;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paging {
        private Integer pageIndex;
        private Integer pageSize;
        private Integer total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hotspot {
        private String key;
        private String rule;
        private String severity;
        private String status;
        private String message;

        @JsonProperty("component")
        private String component;

        @JsonProperty("creationDate")
        private String creationDate;
    }
}
