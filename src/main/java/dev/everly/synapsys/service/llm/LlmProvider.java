package dev.everly.synapsys.service.llm;

import dev.everly.synapsys.service.llm.message.LlmResponse;
import dev.everly.synapsys.service.llm.message.SynapsysRequest;

public interface LlmProvider {
	String getProviderId();

	LlmResponse generate(SynapsysRequest request);
}