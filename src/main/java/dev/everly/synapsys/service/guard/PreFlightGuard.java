package dev.everly.synapsys.service.guard;

import dev.everly.synapsys.service.llm.message.SynapsysRequest;

public abstract class PreFlightGuard implements Guard {
	@Override
	public boolean appliesTo(String sender, GuardPhase phase) {
		return phase == GuardPhase.PREFLIGHT;
	}

	public abstract void inspect(SynapsysRequest request);
}
