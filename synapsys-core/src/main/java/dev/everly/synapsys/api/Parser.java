package dev.everly.synapsys.api;

public interface Parser<RAW_LLM_OUTPUT, PARSED_OUTPUT> {
	PARSED_OUTPUT parse(RAW_LLM_OUTPUT rawLlmOutput) throws ParsingException;
}
