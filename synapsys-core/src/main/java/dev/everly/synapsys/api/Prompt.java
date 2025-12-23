package dev.everly.synapsys.api;
public interface Prompt<REQUEST, PROMPT_MESSAGES_LIST> {
	PROMPT_MESSAGES_LIST generatePrompt(REQUEST request);
}
