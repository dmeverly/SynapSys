package dev.everly.synapsys.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "synapsys")
public record LlmConfig(Llm llm, Security security) {
	public record Llm(String geminiKey, String mistralKey, String defaultModel, String nvdApiKey) {
	}

	public record Security(String clientSecret, String sendersDir) {
	}
}
