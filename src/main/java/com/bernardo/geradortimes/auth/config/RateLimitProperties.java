package com.bernardo.geradortimes.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        int maxRequests,
        long windowSeconds,
        List<EndpointConfig> endpoints
) {

    public RateLimitProperties {
        if (endpoints == null) {
            endpoints = List.of();
        }
    }

    private Map<String, EndpointConfig> endpointMap() {
        if (endpoints.isEmpty()) {
            return Collections.emptyMap();
        }
        return endpoints.stream()
                .collect(Collectors.toMap(EndpointConfig::path, Function.identity()));
    }

    public int maxRequestsFor(String uri) {
        var ep = endpointMap().get(uri);
        return ep != null ? ep.maxRequests() : maxRequests;
    }

    public long windowSecondsFor(String uri) {
        var ep = endpointMap().get(uri);
        return ep != null ? ep.windowSeconds() : windowSeconds;
    }

    public record EndpointConfig(String path, int maxRequests, long windowSeconds) {
    }
}
