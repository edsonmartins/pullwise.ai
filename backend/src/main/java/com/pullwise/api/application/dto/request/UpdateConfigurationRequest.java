package com.pullwise.api.application.dto.request;

import java.util.Map;

public record UpdateConfigurationRequest(
        Map<String, String> configurations
) {}
