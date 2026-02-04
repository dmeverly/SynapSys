package dev.everly.synapsys.service.llm.providers;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import dev.everly.synapsys.service.llm.message.LlmResponse;
import dev.everly.synapsys.service.llm.message.SynapsysRequest;
import dev.everly.synapsys.service.llm.message.TokenUsage;
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
	public LlmResponse generate(SynapsysRequest request) {
		if (delayMs > 0) {
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		TokenUsage usage = new TokenUsage(0, 0, 0);
		return new LlmResponse(request.getContent(), usage, "stub-ollama");
	}
}
