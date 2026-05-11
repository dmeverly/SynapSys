package dev.everly.synapsys.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SynapSysResponse(
        String status,
        String provider,
        String model,
        Object result,
        String error,
        Instant timestamp,
        Map<String, Object> metadata) {

    public static SynapSysResponse ok(String provider, String model, Object result) {
        return new SynapSysResponse("ok", provider, model, result, null, Instant.now(), Map.of());
    }

    public static SynapSysResponse error(String errorMessage) {
        return new SynapSysResponse("error", null, null, null, errorMessage, Instant.now(), Map.of());
    }
}
