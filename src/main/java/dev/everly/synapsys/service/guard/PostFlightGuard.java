package dev.everly.synapsys.service.guard;

import dev.everly.synapsys.service.llm.model.SynapsysRequest;

public interface PostFlightGuard extends Guard {
	String sanitize(SynapsysRequest context, String llmOutput);
}
