package dev.everly.synapsys.api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ParsingStepExecutor<
        REQUEST,
        PROMPT_MESSAGES,
        RAW_LLM_OUTPUT,
        PARSED_OUTPUT
        > implements PipelineStepExecutor<RAW_LLM_OUTPUT, PARSED_OUTPUT, REQUEST> {
    private static final Logger log = LoggerFactory.getLogger(ParsingStepExecutor.class);
    private final Parser<RAW_LLM_OUTPUT, PARSED_OUTPUT> parser;
    private final Repair<RAW_LLM_OUTPUT, ParsingException, RAW_LLM_OUTPUT> parsingRepairMechanism;
    private final int maxParsingAttempts;
    private final Prompt<REQUEST, PROMPT_MESSAGES> promptGenerator;
    private final LlmClient<PROMPT_MESSAGES, RAW_LLM_OUTPUT> llmClient;
    public ParsingStepExecutor(
            Parser<RAW_LLM_OUTPUT, PARSED_OUTPUT> parser,
            Repair<RAW_LLM_OUTPUT, ParsingException, RAW_LLM_OUTPUT> parsingRepairMechanism,
            int maxParsingAttempts,
            Prompt<REQUEST, PROMPT_MESSAGES> promptGenerator,
            LlmClient<PROMPT_MESSAGES, RAW_LLM_OUTPUT> llmClient
    ) {
        if (maxParsingAttempts < 1) {
            throw new IllegalArgumentException("Max parsing attempts must be at least 1.");
        }
        this.parser = parser;
        this.parsingRepairMechanism = parsingRepairMechanism;
        this.maxParsingAttempts = maxParsingAttempts;
        this.promptGenerator = promptGenerator;
        this.llmClient = llmClient;
    }
    @Override
    public PARSED_OUTPUT executeStep(RAW_LLM_OUTPUT initialRawLlmOutput, REQUEST originalRequest) {
        RAW_LLM_OUTPUT currentRawLlmOutput = initialRawLlmOutput;
        for (int attempt = 0; attempt < maxParsingAttempts; attempt++) {
            try {
                return parser.parse(currentRawLlmOutput);
            } catch (ParsingException e) {
                log.warn("Parsing failed on attempt {}/{}: {}", (attempt + 1), maxParsingAttempts, e.getMessage());
                if (parsingRepairMechanism != null && attempt < maxParsingAttempts - 1) {
                    log.info("Attempting parsing repair...");
                    RAW_LLM_OUTPUT repairedRawOutput = parsingRepairMechanism.repair(currentRawLlmOutput, e);
                    if (repairedRawOutput != null) {
                        log.info("Parsing repair returned a new raw output. Re-attempting parse.");
                        currentRawLlmOutput = repairedRawOutput;
                    } else {
                        log.error("Parsing repair mechanism returned null. Cannot proceed with parsing.");
                        throw new RuntimeException("Parsing failed and repair was not possible: " + e.getMessage(), e);
                    }
                } else {
                    log.error("No parsing repair mechanism configured or max attempts reached. Aborting.");
                    throw new RuntimeException("Parsing failed after " + (attempt + 1) + " attempts: " + e.getMessage(), e);
                }
            }
        }
        throw new RuntimeException("Unexpected state: Parsing failed after all attempts without throwing an exception.");
    }
}