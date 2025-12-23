package dev.everly.synapsys.api;

public interface PostProcessor<VALIDATED_INPUT, FINAL_AGENT_RESPONSE> {
	FINAL_AGENT_RESPONSE process(VALIDATED_INPUT validatedInput);
}
