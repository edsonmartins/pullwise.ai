package com.pullwise.api.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO para payload de webhook do BitBucket.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitBucketWebhookPayload {

    private String action;

    @JsonProperty("pullrequest")
    private PullRequest pullRequest;

    @JsonProperty("actor")
    private Actor actor;

    @JsonProperty("repository")
    private Repository repository;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        private Long id;
        private Integer number;
        private String title;
        private String description;
        private String state;

        @JsonProperty("author")
        private Author author;

        @JsonProperty("source")
        private Branch source;

        @JsonProperty("destination")
        private Branch destination;

        @JsonProperty("created_on")
        private String createdAt;

        @JsonProperty("updated_on")
        private String updatedAt;

        @JsonProperty("reviewers")
        private List<Reviewer> reviewers;

        @JsonProperty("links")
        private Links links;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        @JsonProperty("display_name")
        private String displayName;

        private String nickname;
        private String email;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reviewer {
        @JsonProperty("display_name")
        private String displayName;

        private String nickname;
        private String uuid;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Branch {
        private Commit commit;

        @JsonProperty("branch")
        private BranchInfo branch;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BranchInfo {
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit {
        private String hash;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        @JsonProperty("html")
        private HtmlLink html;

        @JsonProperty("commits")
        private CommitsLink commits;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HtmlLink {
        private String href;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitsLink {
        private String href;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Actor {
        private String nickname;
        @JsonProperty("display_name")
        private String displayName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        private String name;
        private String slug;

        @JsonProperty("full_name")
        private String fullName;

        private String type;

        @JsonProperty("is_private")
        private boolean isPrivate;

        private Owner owner;

        private UUID uuid;

        @JsonProperty("links")
        private RepositoryLinks links;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Owner {
        private String username;
        @JsonProperty("display_name")
        private String displayName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UUID {
        private String uuid;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryLinks {
        @JsonProperty("html")
        private HtmlLink html;

        @JsonProperty("commits")
        private CommitsLink commits;
    }
}
