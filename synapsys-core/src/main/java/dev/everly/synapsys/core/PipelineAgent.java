package dev.everly.synapsys.core;
import dev.everly.synapsys.api.*;
public class PipelineAgent<
        REQUEST,
        PROMPT_MESSAGES,
        RAW_LLM_OUTPUT,
        PARSED_OUTPUT,
        FINAL_AGENT_RESPONSE
        > implements Agent<REQUEST, FINAL_AGENT_RESPONSE> {
    private final Prompt<REQUEST, PROMPT_MESSAGES> promptGenerator;
    private final LlmClient<PROMPT_MESSAGES, RAW_LLM_OUTPUT> llmClient;
    private final PipelineStepExecutor<RAW_LLM_OUTPUT, PARSED_OUTPUT, REQUEST> parsingExecutor;
    private final PipelineStepExecutor<PARSED_OUTPUT, PARSED_OUTPUT, REQUEST> validationExecutor;
    private final PostProcessor<PARSED_OUTPUT, FINAL_AGENT_RESPONSE> postProcessor;
    private final LlmCallOptions llmCallOptions;
    public PipelineAgent(
            Prompt<REQUEST, PROMPT_MESSAGES> promptGenerator,
            LlmClient<PROMPT_MESSAGES, RAW_LLM_OUTPUT> llmClient,
            PipelineStepExecutor<RAW_LLM_OUTPUT, PARSED_OUTPUT, REQUEST> parsingExecutor,
            PipelineStepExecutor<PARSED_OUTPUT, PARSED_OUTPUT, REQUEST> validationExecutor,
            PostProcessor<PARSED_OUTPUT, FINAL_AGENT_RESPONSE> postProcessor,
            LlmCallOptions llmCallOptions
    ) {
        this.promptGenerator = promptGenerator;
        this.llmClient = llmClient;
        this.parsingExecutor = parsingExecutor;
        this.validationExecutor = validationExecutor;
        this.postProcessor = postProcessor;
        this.llmCallOptions = llmCallOptions;
    }
    public static <REQ, PM, RAW, PARSED, FINAL> PipelineAgent<REQ, PM, RAW, PARSED, FINAL> create(
            Prompt<REQ, PM> promptGenerator,
            LlmClient<PM, RAW> llmClient,
            Parser<RAW, PARSED> parser,
            Validator<PARSED, ValidationResult> validator,
            PostProcessor<PARSED, FINAL> postProcessor,
            Repair<RAW, ParsingException, RAW> parsingRepairMechanism,
            Repair<PARSED, ValidationResult, PARSED> validationRepairMechanism,
            int maxParsingAttempts,
            int maxValidationAttempts,
            LlmCallOptions llmCallOptions
    ) {
        ParsingStepExecutor<REQ, PM, RAW, PARSED> parsingExecutor = new ParsingStepExecutor<>(
                parser, parsingRepairMechanism, maxParsingAttempts,
                promptGenerator, llmClient
        );
        ValidationStepExecutor<REQ, PM, RAW, PARSED> validationExecutor = new ValidationStepExecutor<>(
                validator, validationRepairMechanism, maxValidationAttempts,
                promptGenerator, llmClient, parser
        );
        return new PipelineAgent<>(
                promptGenerator, llmClient, parsingExecutor, validationExecutor, postProcessor,
                llmCallOptions
        );
    }
    @Override
    public FINAL_AGENT_RESPONSE execute(REQUEST request) {
        PROMPT_MESSAGES promptMessages = promptGenerator.generatePrompt(request);
        RAW_LLM_OUTPUT rawLlmOutput = llmClient.chat(promptMessages, llmCallOptions);
        PARSED_OUTPUT parsedOutput = parsingExecutor.executeStep(rawLlmOutput, request);
        PARSED_OUTPUT validatedOutput = validationExecutor.executeStep(parsedOutput, request);
        return postProcessor.process(validatedOutput);
    }
}