package dev.everly.synapsys.service.llm.model;

public record LlmResult(String content, TokenUsage usage, String providerUsed) {
}
