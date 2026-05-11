package dev.everly.synapsys.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public final class SynapSysRequest {

    private final String provider;
    private final String model;
    private final JsonNode query;
    private final String apiKey;
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonCreator
    public SynapSysRequest(
            @JsonProperty("provider") String provider,
            @JsonProperty("model") String model,
            @JsonProperty("query") JsonNode query,
            @JsonProperty("apiKey") String apiKey) {
        this.provider = requireNonBlank(provider, "provider");
        this.model = requireNonBlank(model, "model");
        this.query = requireValidQuery(query);
        this.apiKey = requireNonBlank(apiKey, "apiKey");
    }

    @JsonAnySetter
    public void addUnknownProperty(String name, Object value) {
        additionalProperties.put(name, value);
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public JsonNode getQuery() {
        return query;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean hasAdditionalProperties() {
        return !additionalProperties.isEmpty();
    }

    private static String requireNonBlank(String value, String field) {
        String trimmed = Objects.requireNonNull(value, field + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    private static JsonNode requireValidQuery(JsonNode value) {
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (!value.isTextual() && !value.isObject()) {
            throw new IllegalArgumentException("query must be a string or object");
        }
        return value;
    }
}