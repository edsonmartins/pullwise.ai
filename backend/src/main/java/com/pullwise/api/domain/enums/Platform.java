package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Plataformas de controle de versão suportadas.
 */
@Getter
public enum Platform {
    /**
     * GitHub - github.com
     */
    GITHUB("github", "GitHub", "github.com"),

    /**
     * BitBucket - bitbucket.org
     */
    BITBUCKET("bitbucket", "BitBucket", "bitbucket.org"),

    /**
     * GitLab - gitlab.com (futuro)
     */
    GITLAB("gitlab", "GitLab", "gitlab.com"),

    /**
     * Azure DevOps - dev.azure.com
     */
    AZURE_DEVOPS("azure_devops", "Azure DevOps", "dev.azure.com");

    private final String code;
    private final String displayName;
    private final String domain;

    Platform(String code, String displayName, String domain) {
        this.code = code;
        this.displayName = displayName;
        this.domain = domain;
    }

    public static Platform fromCode(String code) {
        for (Platform platform : values()) {
            if (platform.code.equalsIgnoreCase(code)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Unknown platform: " + code);
    }

    public static Platform fromUrl(String url) {
        // Azure DevOps also supports *.visualstudio.com (legacy)
        if (url.contains("visualstudio.com")) {
            return AZURE_DEVOPS;
        }
        for (Platform platform : values()) {
            if (url.contains(platform.domain)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Cannot determine platform from URL: " + url);
    }
}
