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
public class StubGeminiProvider extends GeminiStrategy {

	private final long delayMs;

	public StubGeminiProvider(String provider, String model, String api_key, String query) {
		super(provider, model, api_key, query);
		this.delayMs = Long.parseLong(System.getProperty("stub.delayMs", "0"));
		log.warn(LogColor.test("Stub Gemini Provider CREATED"));
		log.warn(LogColor.test("LLM CALLS DISABLED"));
	}

	public String getProviderId() {
		return "gemini stub";
	}

	public SynapSysResponse generate(String model, JsonNode query, String apiKey) {
		if (delayMs > 0) {
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return SynapSysResponse.ok("stub-gemini", model, query);
	}
}