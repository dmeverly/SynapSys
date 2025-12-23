package dev.everly.synapsys.agents;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.everly.synapsys.api.*;
import dev.everly.synapsys.core.PipelineAgent;
import dev.everly.synapsys.core.SynapSysManager;
import dev.everly.synapsys.core.repair.JsonGuardsParsingRepair;
import dev.everly.synapsys.core.repair.NoOpValidationRepair;
import dev.everly.synapsys.llm.gemini.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
public class GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE>
        implements Agent<REQUEST, FINAL_AGENT_RESPONSE> {
    private static final Logger log = LoggerFactory.getLogger(GeminiAgent.class);
    private Prompt<REQUEST, List<MessageModel>> promptGenerator;
    private Parser<String, PARSED_OUTPUT> parser;
    private Validator<PARSED_OUTPUT, ValidationResult> validator;
    private PostProcessor<PARSED_OUTPUT, FINAL_AGENT_RESPONSE> postProcessor;
    private Repair<String, ParsingException, String> parsingRepairMechanism;
    private Repair<PARSED_OUTPUT, ValidationResult, PARSED_OUTPUT> validationRepairMechanism;
    private int maxParsingAttempts = 3;
    private int maxValidationAttempts = 3;
    private LlmCallOptions.Builder llmCallOptionsBuilder = LlmCallOptions.builder();
    private String llmClientId = "gemini-default";
    private ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<PipelineAgent<REQUEST, List<MessageModel>, String, PARSED_OUTPUT, FINAL_AGENT_RESPONSE>> pipelineAgentRef = new AtomicReference<>();
    public GeminiAgent() {
        this.parsingRepairMechanism = new JsonGuardsParsingRepair(this.objectMapper);
        this.validationRepairMechanism = new NoOpValidationRepair<>();
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withPromptGenerator(Prompt<REQUEST, List<MessageModel>> promptGenerator) {
        ensureNotBuilt();
        this.promptGenerator = Objects.requireNonNull(promptGenerator, "PromptGenerator cannot be null.");
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withSystemPrompt(String systemPrompt) {
        ensureNotBuilt();
        Objects.requireNonNull(systemPrompt, "System prompt cannot be null.");
        this.promptGenerator = request -> List.of(
                new MessageModel("system", systemPrompt),
                new MessageModel("user", request.toString())
        );
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withParser(Parser<String, PARSED_OUTPUT> parser) {
        ensureNotBuilt();
        this.parser = Objects.requireNonNull(parser, "Parser cannot be null.");
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withValidator(Validator<PARSED_OUTPUT, ValidationResult> validator) {
        ensureNotBuilt();
        this.validator = Objects.requireNonNull(validator, "Validator cannot be null.");
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withPostProcessor(PostProcessor<PARSED_OUTPUT, FINAL_AGENT_RESPONSE> postProcessor) {
        ensureNotBuilt();
        this.postProcessor = Objects.requireNonNull(postProcessor, "PostProcessor cannot be null.");
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withParsingRepairMechanism(Repair<String, ParsingException, String> parsingRepairMechanism) {
        ensureNotBuilt();
        this.parsingRepairMechanism = Objects.requireNonNull(parsingRepairMechanism, "Parsing repair mechanism cannot be null.");
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withValidationRepairMechanism(Repair<PARSED_OUTPUT, ValidationResult, PARSED_OUTPUT> validationRepairMechanism) {
        ensureNotBuilt();
        this.validationRepairMechanism = Objects.requireNonNull(validationRepairMechanism, "Validation repair mechanism cannot be null.");
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withMaxParsingAttempts(int maxParsingAttempts) {
        ensureNotBuilt();
        if (maxParsingAttempts < 1) {
            throw new IllegalArgumentException("Max parsing attempts must be at least 1.");
        }
        this.maxParsingAttempts = maxParsingAttempts;
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withMaxValidationAttempts(int maxValidationAttempts) {
        ensureNotBuilt();
        if (maxValidationAttempts < 1) {
            throw new IllegalArgumentException("Max validation attempts must be at least 1.");
        }
        this.maxValidationAttempts = maxValidationAttempts;
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withLlmClientId(String llmClientId) {
        ensureNotBuilt();
        if (llmClientId == null || llmClientId.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM Client ID cannot be null or empty.");
        }
        this.llmClientId = llmClientId;
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withObjectMapper(ObjectMapper objectMapper) {
        ensureNotBuilt();
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null.");
        if (this.parsingRepairMechanism instanceof JsonGuardsParsingRepair) {
            this.parsingRepairMechanism = new JsonGuardsParsingRepair(objectMapper);
        }
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withJsonMode(boolean jsonMode) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.jsonMode(jsonMode);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withTemperature(Double temperature) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.temperature(temperature);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withTopP(Double topP) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.topP(topP);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withMaxTokens(Integer maxTokens) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.maxTokens(maxTokens);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withStopSequences(List<String> stopSequences) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.stopSequences(stopSequences);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withSeed(Long seed) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.seed(seed);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withStream(Boolean stream) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.stream(stream);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> withLlmCustomOption(String key, Object value) {
        ensureNotBuilt();
        this.llmCallOptionsBuilder.customOption(key, value);
        return this;
    }
    public GeminiAgent<REQUEST, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> build() {
        if (pipelineAgentRef.get() == null) {
            PipelineAgent<REQUEST, List<MessageModel>, String, PARSED_OUTPUT, FINAL_AGENT_RESPONSE> currentAgent = pipelineAgentRef.get();
            if (currentAgent == null) {
                synchronized (this) {
                    currentAgent = pipelineAgentRef.get();
                    if (currentAgent == null) {
                        validateConfiguration();
                        LlmClient<List<MessageModel>, String> llmClient = SynapSysManager.getInstance()
                                .getComponent(llmClientId, GeminiClient.class);
                        if (llmClient == null) {
                            throw new IllegalStateException("GeminiClient with ID '" + llmClientId + "' not found in SynapSysManager. " +
                                    "Ensure it's registered and initialized before building the agent.");
                        }
                        LlmCallOptions finalLlmCallOptions = llmCallOptionsBuilder.build();
                        currentAgent = PipelineAgent.create(
                                promptGenerator,
                                llmClient,
                                parser,
                                validator,
                                postProcessor,
                                parsingRepairMechanism,
                                validationRepairMechanism,
                                maxParsingAttempts,
                                maxValidationAttempts,
                                finalLlmCallOptions
                        );
                        pipelineAgentRef.set(currentAgent);
                        log.info("GeminiAgent built successfully with client ID: {}", llmClientId);
                    }
                }
            }
        }
        return this;
    }
    @Override
    public FINAL_AGENT_RESPONSE execute(REQUEST request) {
        if (pipelineAgentRef.get() == null) {
            build();
        }
        return pipelineAgentRef.get().execute(request);
    }
    private void validateConfiguration() {
        if (promptGenerator == null) throw new IllegalStateException("PromptGenerator must be provided.");
        if (parser == null) throw new IllegalStateException("Parser must be provided.");
        if (validator == null) throw new IllegalStateException("Validator must be provided.");
        if (postProcessor == null) throw new IllegalStateException("PostProcessor must be provided.");
    }
    private void ensureNotBuilt() {
        if (pipelineAgentRef.get() != null) {
            throw new IllegalStateException("Agent has already been built. Configuration cannot be changed after build() or first execute().");
        }
    }
}