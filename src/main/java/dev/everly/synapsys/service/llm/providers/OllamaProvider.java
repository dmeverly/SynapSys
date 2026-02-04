package dev.everly.synapsys.service.llm.providers;

import static dev.everly.synapsys.service.llm.LlmProviderException.Type.*;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.everly.synapsys.config.LlmConfig;
import dev.everly.synapsys.service.llm.LlmProviderException;
import dev.everly.synapsys.service.llm.message.LlmResponse;
import dev.everly.synapsys.service.llm.message.SynapsysRequest;
import dev.everly.synapsys.service.llm.message.TokenUsage;
import dev.everly.synapsys.util.LogColor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Profile("!test")
public class OllamaProvider implements LlmProvider {

	private final RestClient restClient;
	private final String baseUrl;
	private final String defaultModel;

	public OllamaProvider(LlmConfig config) {
		String configuredModel = (config.llm() != null && config.llm().defaultModel() != null)
				? config.llm().defaultModel().trim()
				: "";
		this.defaultModel = configuredModel.isBlank() ? "qwen3:8b" : configuredModel;

		String configuredBaseUrl = "";
		try {
			configuredBaseUrl = (config.llm() != null && config.llm().ollamaBaseUrl() != null)
					? config.llm().ollamaBaseUrl().trim()
					: "";
		} catch (Exception ignored) {
		}

		this.baseUrl = configuredBaseUrl.isBlank() ? "http://localhost:11434" : configuredBaseUrl;

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
		requestFactory.setReadTimeout((int) Duration.ofSeconds(180).toMillis());

		this.restClient = RestClient.builder().baseUrl(this.baseUrl)
				.defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).requestFactory(requestFactory).build();

		log.warn(LogColor.live("LIVE OLLAMA PROVIDER CREATED"));
		log.warn(LogColor.live("LOCAL-ONLY CALLS ENABLED @ " + this.baseUrl));
	}

	private static String safeMessage(RestClientResponseException ex) {
		String body = ex.getResponseBodyAsString();
		if (body == null || body.isBlank()) {
			return truncate(ex.getMessage());
		}
		return truncate(body);
	}

	private static String truncate(String s) {
		if (s == null) {
			return "";
		}
		String t = s.replaceAll("\\s+", " ").trim();
		return t.length() > 500 ? t.substring(0, 500) + "..." : t;
	}

	@Override
	public String getProviderId() {
		return "ollama";
	}

	@Override
	public LlmResponse generate(SynapsysRequest request) {
		String resolvedModel = request.getModelVersion().isBlank() ? defaultModel : request.getModelVersion().trim();

		OllamaChatRequest payload = new OllamaChatRequest(resolvedModel, request.getSystemInstruction(),
				List.of(new OllamaMessage("user", request.getContent())), false);

		try {
			OllamaChatResponse response = restClient.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON).body(payload).retrieve().body(OllamaChatResponse.class);

			if (response == null || response.message == null) {
				throw new LlmProviderException(UNKNOWN, "Ollama returned an empty response body.", null);
			}

			String text = response.message.content == null ? "" : response.message.content;
			TokenUsage usage = extractUsage(response);

			return new LlmResponse(text, usage, getProviderId());

		} catch (RestClientResponseException ex) {
			throw mapHttpError(ex);
		} catch (LlmProviderException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new LlmProviderException(UNKNOWN, "Ollama Failure: " + ex.getMessage(), ex);
		}
	}

	private TokenUsage extractUsage(OllamaChatResponse r) {
		int prompt = r.promptEvalCount == null ? 0 : r.promptEvalCount;
		int completion = r.evalCount == null ? 0 : r.evalCount;
		return new TokenUsage(prompt, completion, prompt + completion);
	}

	private LlmProviderException mapHttpError(RestClientResponseException ex) {
		int code = ex.getStatusCode().value();
		String msg = safeMessage(ex);

		return switch (code) {
		case 400 -> new LlmProviderException(INVALID_REQUEST, "Ollama rejected request: " + msg, ex);
		case 401, 403 -> new LlmProviderException(KEY, "Ollama authentication/permission error: " + msg, ex);
		case 404 ->
			new LlmProviderException(UNAVAILABLE, "Ollama not found or model missing at " + baseUrl + ": " + msg, ex);
		case 429 -> new LlmProviderException(RATE_LIMIT, "Ollama rate-limited the request: " + msg, ex);
		case 500, 502, 503 -> new LlmProviderException(UNAVAILABLE, "Ollama unavailable: " + msg, ex);
		default -> new LlmProviderException(UNKNOWN, "Ollama HTTP " + code + ": " + msg, ex);
		};
	}

	record OllamaChatRequest(String model, String system, List<OllamaMessage> messages, boolean stream) {
	}

	record OllamaMessage(String role, String content) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class OllamaChatResponse {
		public OllamaMessage message;

		@JsonProperty("prompt_eval_count")
		public Integer promptEvalCount;

		@JsonProperty("eval_count")
		public Integer evalCount;
	}
}
