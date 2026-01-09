package dev.everly.synapsys.service.llm;

import dev.everly.synapsys.service.llm.model.LlmResult;
import dev.everly.synapsys.service.llm.model.SynapsysRequest;

public interface LlmProvider {
	String getProviderId();

	LlmResult generate(SynapsysRequest request);
}