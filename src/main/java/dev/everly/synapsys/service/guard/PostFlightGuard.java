package dev.everly.synapsys.service.guard;

import dev.everly.synapsys.service.llm.message.SynapsysRequest;

public abstract class PostFlightGuard implements Guard {
	@Override
	public boolean appliesTo(String sender, GuardPhase phase) {
		return phase == GuardPhase.POSTFLIGHT;
	}

	public abstract String sanitize(SynapsysRequest context, String llmOutput);
}
