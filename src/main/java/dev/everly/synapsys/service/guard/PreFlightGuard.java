package dev.everly.synapsys.service.guard;

import dev.everly.synapsys.service.llm.message.SynapsysRequest;

public interface PreFlightGuard extends Guard {
	void inspect(SynapsysRequest request);
}
