package dev.everly.synapsys.service.guard;

public interface Guard {
	default boolean appliesTo(String sender, GuardPhase phase) {
		return true;
	}
}
