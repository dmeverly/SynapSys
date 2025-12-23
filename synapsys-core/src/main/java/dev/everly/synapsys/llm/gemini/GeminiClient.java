package dev.everly.synapsys.llm.gemini;
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
public class GeminiClient extends SynapSysComponentBase
        implements LlmClient<List<MessageModel>, String>, ISynapSysComponent {
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    public static final String PARAM_BASE_URL = "gemini.baseUrl";
    public static final String PARAM_API_KEY = "gemini.apiKey";
    public static final String PARAM_MODEL_NAME = "gemini.modelName";
    private String baseUrl;
    private String apiKey;
    private String model;
    private HttpClient http;
    private final ObjectMapper objectMapper;
    public GeminiClient(String id, String name, ObjectMapper objectMapper) {
        super(id, name);
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null.");
    }
    @Override
    public void initialize() throws Exception {
        log.info("Initializing GeminiClient component: {} (ID: {})", getName(), getID());
        ConfigurationService configService = SynapSysManager.getInstance()
                .getComponent(ConfigurationService.COMPONENT_ID, ConfigurationService.class);
        if (configService == null) {
            throw new IllegalStateException("ConfigurationService not found. Ensure it's registered and initialized.");
        }
        this.baseUrl = Optional.ofNullable(getParameter(PARAM_BASE_URL, String.class))
                .or(() -> configService.getProperty(PARAM_BASE_URL))
                .orElse("https://generativelanguage.googleapis.com/v1beta");
        this.model = Optional.ofNullable(getParameter(PARAM_MODEL_NAME, String.class))
                .or(() -> configService.getProperty(PARAM_MODEL_NAME))
                .orElse("gemini-2.5-flash-lite");
        this.apiKey = Optional.ofNullable(getParameter(PARAM_API_KEY, String.class))
                .or(() -> configService.getApiKey("GEMINI"))
                .orElseThrow(() -> new IllegalStateException("Gemini API key not found for component " + getID()));
        this.baseUrl = this.baseUrl.endsWith("/") ? this.baseUrl.substring(0, this.baseUrl.length() - 1) : this.baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        log.info("GeminiClient initialized. Base URL: {}, Model: {}", baseUrl, model);
    }
    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down GeminiClient component: {} (ID: {})", getName(), getID());
        this.http = null;
        this.apiKey = null;
        this.baseUrl = null;
        this.model = null;
    }
    @Override
    public String chat(List<MessageModel> promptMessages, LlmCallOptions options) {
        try {
            String payload = buildPayload(promptMessages, options);
            String endpoint = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);
            HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60)).header("Content-Type", "application/json");
            HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                String errorBody = resp.body();
                String errorMessage = String.format("Gemini LLM HTTP error %d for model %s: %s", resp.statusCode(), model, errorBody);
                log.error(errorMessage);
                if (resp.statusCode() == 401) {
                    throw new LlmAuthenticationException("Invalid Gemini API key or insufficient permissions.", errorBody);
                } else if (resp.statusCode() == 429) {
                    throw new LlmRateLimitException("Gemini LLM rate limit exceeded.", errorBody);
                } else {
                    throw new LlmApiException(errorMessage, resp.statusCode(), errorBody);
                }
            }
            return extractContent(resp.body());
        } catch (LlmClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini LLM request failed unexpectedly", e);
            throw new LlmClientException("Gemini LLM request failed unexpectedly", e);
        }
    }
    private String buildPayload(List<MessageModel> messages, LlmCallOptions options) throws JsonProcessingException {
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode contentsNode = objectMapper.createArrayNode();
        boolean jsonModeInstructionAdded = false;
        for (MessageModel message : messages) {
            ObjectNode contentNode = objectMapper.createObjectNode();
            String role = message.getRole().toLowerCase();
            String contentText = message.getContent();
            if ("system".equals(role)) {
                role = "user";
            }
            if (options.isJsonMode() && !jsonModeInstructionAdded) {
                contentText = "Respond only with a JSON object. Do not include any conversational text outside the JSON.\n" + contentText;
                jsonModeInstructionAdded = true;
            }
            contentNode.put("role", role);
            ArrayNode partsNode = objectMapper.createArrayNode();
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("text", contentText);
            partsNode.add(textPart);
            contentNode.set("parts", partsNode);
            contentsNode.add(contentNode);
        }
        rootNode.set("contents", contentsNode);
        ObjectNode generationConfigNode = objectMapper.createObjectNode();
        if (options.getTemperature() != null) {
            generationConfigNode.put("temperature", options.getTemperature());
        }
        if (options.getTopP() != null) {
            generationConfigNode.put("topP", options.getTopP());
        }
        if (options.getMaxTokens() != null) {
            generationConfigNode.put("maxOutputTokens", options.getMaxTokens());
        }
        if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
            ArrayNode stopSequencesNode = objectMapper.createArrayNode();
            options.getStopSequences().forEach(stopSequencesNode::add);
            generationConfigNode.set("stopSequences", stopSequencesNode);
        }
        if (!generationConfigNode.isEmpty()) {
            rootNode.set("generationConfig", generationConfigNode);
        }
        return objectMapper.writeValueAsString(rootNode);
    }
    private String extractContent(String body) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(body);
        JsonNode contentNode = rootNode.at("/candidates/0/content/parts/0/text");
        if (contentNode.isMissingNode()) {
            log.warn("Could not find 'candidates[0].content.parts[0].text' in Gemini LLM response: {}", body);
            JsonNode textNode = rootNode.findValue("text");
            if (textNode != null && textNode.isTextual()) {
                return textNode.asText();
            }
            return body;
        }
        return contentNode.asText();
    }
}