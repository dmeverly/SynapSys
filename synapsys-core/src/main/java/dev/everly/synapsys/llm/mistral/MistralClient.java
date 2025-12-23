package dev.everly.synapsys.llm.mistral;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
public class MistralClient extends SynapSysComponentBase
        implements LlmClient<List<MessageModel>, String>, ISynapSysComponent {
    private static final Logger log = LoggerFactory.getLogger(MistralClient.class);
    public static final String PARAM_BASE_URL = "mistral.baseUrl";
    public static final String PARAM_API_KEY = "mistral.apiKey";
    public static final String PARAM_MODEL_NAME = "mistral.modelName";
    private String baseUrl;
    private String apiKey;
    private String model;
    private HttpClient http;
    private final ObjectMapper objectMapper;
    public MistralClient(String id, String name, ObjectMapper objectMapper) {
        super(id, name);
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null.");
    }
    @Override
    public void initialize() throws Exception {
        log.info("Initializing MistralClient component: {} (ID: {})", getName(), getID());
        ConfigurationService configService = SynapSysManager.getInstance()
                .getComponent(ConfigurationService.COMPONENT_ID, ConfigurationService.class);
        if (configService == null) {
            throw new IllegalStateException("ConfigurationService not found. Ensure it's registered and initialized.");
        }
        this.baseUrl = Optional.ofNullable(getParameter(PARAM_BASE_URL, String.class))
                .or(() -> configService.getProperty(PARAM_BASE_URL))
                .orElse("https://api.mistral.ai");
        this.model = Optional.ofNullable(getParameter(PARAM_MODEL_NAME, String.class))
                .or(() -> configService.getProperty(PARAM_MODEL_NAME))
                .orElse("mistral-small-latest");
        this.apiKey = Optional.ofNullable(getParameter(PARAM_API_KEY, String.class))
                .or(() -> configService.getApiKey("MISTRAL"))
                .orElseThrow(() -> new IllegalStateException("Mistral API key not found for component " + getID()));
        this.baseUrl = this.baseUrl.endsWith("/") ? this.baseUrl.substring(0, this.baseUrl.length() - 1) : this.baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        log.info("MistralClient initialized. Base URL: {}, Model: {}", baseUrl, model);
    }
    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down MistralClient component: {} (ID: {})", getName(), getID());
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
                String errorMessage = String.format("Mistral LLM HTTP error %d for model %s: %s", resp.statusCode(), model, errorBody);
                log.error(errorMessage);
                if (resp.statusCode() == 401) {
                    throw new LlmAuthenticationException("Invalid Mistral API key or insufficient permissions.", errorBody);
                } else if (resp.statusCode() == 429) {
                    throw new LlmRateLimitException("Mistral LLM rate limit exceeded.", errorBody);
                } else {
                    throw new LlmApiException(errorMessage, resp.statusCode(), errorBody);
                }
            }
            return extractContent(resp.body());
        } catch (LlmClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Mistral LLM request failed unexpectedly", e);
            throw new LlmClientException("Mistral LLM request failed unexpectedly", e);
        }
    }
    private String buildPayload(List<MessageModel> messages, LlmCallOptions options) throws JsonProcessingException {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("model", model);
        List<MessageModel> messagesForPayload = new ArrayList<>(messages);
        if (options.isJsonMode()) {
            boolean instructionAdded = false;
            for (int i = 0; i < messagesForPayload.size(); i++) {
                MessageModel msg = messagesForPayload.get(i);
                if ("system".equalsIgnoreCase(msg.getRole())) {
                    messagesForPayload.set(i, new MessageModel(msg.getRole(),
                            "Respond only with a JSON object. Do not include any conversational text outside the JSON.\n" + msg.getContent()));
                    instructionAdded = true;
                    break;
                }
            }
            if (!instructionAdded) {
                messagesForPayload.add(0, new MessageModel("system", "Respond only with a JSON object. Do not include any conversational text outside the JSON."));
            }
        }
        ArrayNode messagesNode = objectMapper.createArrayNode();
        for (MessageModel message : messagesForPayload) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", message.getRole());
            messageNode.put("content", message.getContent());
            messagesNode.add(messageNode);
        }
        rootNode.set("messages", messagesNode);
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
            rootNode.put("random_seed", options.getSeed());
        }
        return objectMapper.writeValueAsString(rootNode);
    }
    private String extractContent(String body) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(body);
        JsonNode contentNode = rootNode.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode()) {
            log.warn("Could not find 'choices[0].message.content' in Mistral LLM response: {}", body);
            return body;
        }
        return contentNode.asText();
    }
}