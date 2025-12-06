package com.hmiso.examples.samldemo;

public record SamlDemoProperties(String sessionAttributeKey) {
    public SamlDemoProperties {
        if (sessionAttributeKey == null || sessionAttributeKey.isBlank()) {
            throw new IllegalArgumentException("sessionAttributeKey is required");
        }
    }
}
