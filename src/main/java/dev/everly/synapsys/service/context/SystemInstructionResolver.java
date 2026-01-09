package dev.everly.synapsys.service.context;

import dev.everly.synapsys.service.llm.model.SynapsysRequest;

public interface SystemInstructionResolver {
	boolean appliesTo(String sender);

	String resolve(SynapsysRequest request);
}
