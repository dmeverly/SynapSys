package dev.everly.synapsys.api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ValidationStepExecutor<
        REQUEST,
        PROMPT_MESSAGES,
        RAW_LLM_OUTPUT,
        PARSED_OUTPUT
        > implements PipelineStepExecutor<PARSED_OUTPUT, PARSED_OUTPUT, REQUEST> {
    private static final Logger log = LoggerFactory.getLogger(ValidationStepExecutor.class);
    private final Validator<PARSED_OUTPUT, ValidationResult> validator;
    private final Repair<PARSED_OUTPUT, ValidationResult, PARSED_OUTPUT> validationRepairMechanism;
    private final int maxValidationAttempts;
    private final Prompt<REQUEST, PROMPT_MESSAGES> promptGenerator;
    private final LlmClient<PROMPT_MESSAGES, RAW_LLM_OUTPUT> llmClient;
    private final Parser<RAW_LLM_OUTPUT, PARSED_OUTPUT> parser;
    public ValidationStepExecutor(
            Validator<PARSED_OUTPUT, ValidationResult> validator,
            Repair<PARSED_OUTPUT, ValidationResult, PARSED_OUTPUT> validationRepairMechanism,
            int maxValidationAttempts,
            Prompt<REQUEST, PROMPT_MESSAGES> promptGenerator,
            LlmClient<PROMPT_MESSAGES, RAW_LLM_OUTPUT> llmClient,
            Parser<RAW_LLM_OUTPUT, PARSED_OUTPUT> parser
    ) {
        if (maxValidationAttempts < 1) {
            throw new IllegalArgumentException("Max validation attempts must be at least 1.");
        }
        this.validator = validator;
        this.validationRepairMechanism = validationRepairMechanism;
        this.maxValidationAttempts = maxValidationAttempts;
        this.promptGenerator = promptGenerator;
        this.llmClient = llmClient;
        this.parser = parser;
    }
    @Override
    public PARSED_OUTPUT executeStep(PARSED_OUTPUT initialParsedOutput, REQUEST originalRequest) {
        PARSED_OUTPUT currentParsedOutput = initialParsedOutput;
        for (int attempt = 0; attempt < maxValidationAttempts; attempt++) {
            ValidationResult validationResult = validator.validate(currentParsedOutput);
            if (validationResult.isSuccess()) {
                return currentParsedOutput;
            } else {
                log.warn("Validation failed on attempt {}/{}: {}", (attempt + 1), maxValidationAttempts, validationResult.getErrors());
                if (validationRepairMechanism != null && attempt < maxValidationAttempts - 1) {
                    log.info("Attempting validation repair...");
                    PARSED_OUTPUT repairedOutput = validationRepairMechanism.repair(currentParsedOutput, validationResult);
                    if (repairedOutput != null) {
                        log.info("Validation repair returned a new output. Re-attempting validation.");
                        currentParsedOutput = repairedOutput;
                    } else {
                        log.error("Validation repair mechanism returned null. Cannot proceed with validation.");
                        throw new RuntimeException("Validation failed and repair was not possible: " + validationResult.getErrors());
                    }
                } else {
                    log.error("No validation repair mechanism configured or max attempts reached. Aborting.");
                    throw new RuntimeException("Validation failed after " + (attempt + 1) + " attempts: " + validationResult.getErrors());
                }
            }
        }
        throw new RuntimeException("Unexpected state: Validation failed after all attempts without throwing an exception.");
    }
}