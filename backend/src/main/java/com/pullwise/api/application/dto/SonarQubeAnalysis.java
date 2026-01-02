package com.pullwise.api.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * DTO para análise/relatório do SonarQube.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarQubeAnalysis {

    private String key;
    private String name;
    private String status;
    private String type;

    @JsonProperty("analysisDate")
    private String analysisDate;

    @JsonProperty("revision")
    private String revision;

    @JsonProperty("qualityGate")
    private QualityGate qualityGate;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QualityGate {
        private String status;
        private String condition;

        @JsonProperty("conditions")
        private java.util.List<Condition> conditions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        private String metric;
        private String operator;
        private String status;

        @JsonProperty("errorThreshold")
        private String errorThreshold;

        @JsonProperty("actualValue")
        private String actualValue;
    }
}
