package dev.everly.synapsys.llm.openai;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.everly.synapsys.api.*;
import dev.everly.synapsys.core.ConfigurationService;
import dev.everly.synapsys.core.ISynapSysComponent;
import dev.everly.synapsys.core.SynapSysComponentBase;
import dev.everly.synapsys.core.SynapSysManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
public class OpenAiClient extends SynapSysComponentBase
        implements LlmClient<List<MessageModel>, String>, ISynapSysComponent {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    public static final String PARAM_BASE_URL = "openai.baseUrl";
    public static final String PARAM_API_KEY = "openai.apiKey";
    public static final String PARAM_MODEL_NAME = "openai.modelName";
    private String baseUrl;
    private String apiKey;
    private String model;
    private HttpClient http;
    private final ObjectMapper objectMapper;
    public OpenAiClient(String id, String name, ObjectMapper objectMapper) {
        super(id, name);
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null.");
    }
    @Override
    public void initialize() throws Exception {
        log.info("Initializing OpenAiClient component: {} (ID: {})", getName(), getID());
        ConfigurationService configService = SynapSysManager.getInstance()
                .getComponent(ConfigurationService.COMPONENT_ID, ConfigurationService.class);
        if (configService == null) {
            throw new IllegalStateException("ConfigurationService not found. Ensure it's registered and initialized.");
        }
        this.baseUrl = Optional.ofNullable(getParameter(PARAM_BASE_URL, String.class))
                .or(() -> configService.getProperty(PARAM_BASE_URL))
                .orElse("https://api.openai.com");
        this.model = Optional.ofNullable(getParameter(PARAM_MODEL_NAME, String.class))
                .or(() -> configService.getProperty(PARAM_MODEL_NAME))
                .orElse("gpt-3.5-turbo");
        this.apiKey = Optional.ofNullable(getParameter(PARAM_API_KEY, String.class))
                .or(() -> configService.getApiKey("OPENAI"))
                .orElseThrow(() -> new IllegalStateException("OpenAI API key not found for component " + getID()));
        this.baseUrl = this.baseUrl.endsWith("/") ? this.baseUrl.substring(0, this.baseUrl.length() - 1) : this.baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        log.info("OpenAiClient initialized. Base URL: {}, Model: {}", baseUrl, model);
    }
    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down OpenAiClient component: {} (ID: {})", getName(), getID());
        this.http = null;
        this.apiKey = null;
        this.baseUrl = null;
        this.model = null;
    }
    @Override
    public String chat(List<MessageModel> promptMessages, LlmCallOptions options) {
        try {
            String payload = buildPayload(promptMessages, options);
            HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(60)).header("Content-Type", "application/json");
            if (!apiKey.isBlank()) {
                b.header("Authorization", "Bearer " + apiKey);
            }
            HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                String errorBody = resp.body();
                String errorMessage = String.format("OpenAI LLM HTTP error %d for model %s: %s", resp.statusCode(), model, errorBody);
                log.error(errorMessage);
                if (resp.statusCode() == 401) {
                    throw new LlmAuthenticationException("Invalid OpenAI API key or insufficient permissions.", errorBody);
                } else if (resp.statusCode() == 429) {
                    throw new LlmRateLimitException("OpenAI LLM rate limit exceeded.", errorBody);
                } else {
                    throw new LlmApiException(errorMessage, resp.statusCode(), errorBody);
                }
            }
            return extractContent(resp.body());
        } catch (LlmClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI LLM request failed unexpectedly", e);
            throw new LlmClientException("OpenAI LLM request failed unexpectedly", e);
        }
    }
    private String buildPayload(List<MessageModel> messages, LlmCallOptions options) throws JsonProcessingException {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("model", model);
        ArrayNode messagesNode = objectMapper.createArrayNode();
        for (MessageModel message : messages) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", message.getRole());
            messageNode.put("content", message.getContent());
            messagesNode.add(messageNode);
        }
        rootNode.set("messages", messagesNode);
        if (options.isJsonMode()) {
            ObjectNode responseFormatNode = objectMapper.createObjectNode();
            responseFormatNode.put("type", "json_object");
            rootNode.set("response_format", responseFormatNode);
        }
        if (options.getTemperature() != null) {
            rootNode.put("temperature", options.getTemperature());
        }
        if (options.getTopP() != null) {
            rootNode.put("top_p", options.getTopP());
        }
        if (options.getMaxTokens() != null) {
            rootNode.put("max_tokens", options.getMaxTokens());
        }
        if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
            ArrayNode stopNode = objectMapper.createArrayNode();
            options.getStopSequences().forEach(stopNode::add);
            rootNode.set("stop", stopNode);
        }
        if (options.getSeed() != null) {
            rootNode.put("seed", options.getSeed());
        }
        return objectMapper.writeValueAsString(rootNode);
    }
    private String extractContent(String body) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(body);
        JsonNode contentNode = rootNode.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode()) {
            log.warn("Could not find 'choices[0].message.content' in OpenAI LLM response: {}", body);
            return body;
        }
        return contentNode.asText();
    }
}