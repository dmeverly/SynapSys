package dev.everly.synapsys.api;

public interface LlmClient<PROMPT_MESSAGES_LIST, LLM_RAW_RESPONSE> {
	LLM_RAW_RESPONSE chat(PROMPT_MESSAGES_LIST promptMessagesList, LlmCallOptions options);
}
