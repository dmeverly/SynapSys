package dev.everly.synapsys.service.llm.message;

public record LlmResponse(String content, TokenUsage usage, String providerUsed) {
}
