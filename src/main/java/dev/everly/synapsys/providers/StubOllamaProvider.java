package dev.everly.synapsys.providers;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import dev.everly.synapsys.dto.SynapSysResponse;
import dev.everly.synapsys.util.LogColor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Profile("test")
public class StubOllamaProvider implements LlmProvider {

	private final long delayMs;

	public StubOllamaProvider() {
		this.delayMs = Long.parseLong(System.getProperty("stub.delayMs", "0"));
		log.warn(LogColor.test("Stub Ollama Provider CREATED"));
		log.warn(LogColor.test("LLM CALLS DISABLED"));
	}

	@Override
	public String getProviderId() {
		return "ollama";
	}

	@Override
	public SynapSysResponse generate(String model, JsonNode query, String apiKey) {
		if (delayMs > 0) {
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return SynapSysResponse.ok("stub-ollama", model, query);
	}
}
