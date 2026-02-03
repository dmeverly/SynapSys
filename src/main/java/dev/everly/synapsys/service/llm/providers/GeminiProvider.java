package dev.everly.synapsys.service.llm.providers;

import static dev.everly.synapsys.service.llm.LlmProviderException.Type.*;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import dev.everly.synapsys.config.LlmConfig;
import dev.everly.synapsys.service.llm.ContextKeys;
import dev.everly.synapsys.service.llm.GeminiFileSearchClient;
import dev.everly.synapsys.service.llm.LlmProviderException;
import dev.everly.synapsys.service.llm.message.LlmResponse;
import dev.everly.synapsys.service.llm.message.SynapsysRequest;
import dev.everly.synapsys.service.llm.message.TokenUsage;
import dev.everly.synapsys.util.LogColor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Profile("!test")
public class GeminiProvider implements LlmProvider {

	private final Client geminiSdkClient;
	private final String defaultModel;
	private final GeminiFileSearchClient fileSearchClient;

	public GeminiProvider(LlmConfig config, GeminiFileSearchClient fileSearchClient) {
		String apiKey = (config.llm() != null && config.llm().geminiKey() != null) ? config.llm().geminiKey().trim()
				: "";
		if (apiKey.isBlank()) {
			throw new IllegalStateException("Missing Gemini API Key in configuration.");
		}

		String configuredModel = config.llm().defaultModel();
		this.defaultModel = (configuredModel == null) ? "" : configuredModel.trim();
		this.geminiSdkClient = Client.builder().apiKey(apiKey).build();
		this.fileSearchClient = fileSearchClient;

		log.warn(LogColor.live("LIVE GEMINI PROVIDER CREATED"));
		log.warn(LogColor.live("NETWORK CALLS ENABLED"));
	}

	@Override
	public String getProviderId() {
		return "gemini";
	}

	@Override
	public LlmResponse generate(SynapsysRequest synapsysRequest) {
		String resolvedModel = synapsysRequest.getModelVersion().isBlank() ? defaultModel
				: synapsysRequest.getModelVersion();

		String storeName = readFileSearchStoreName(synapsysRequest.getContext());
		boolean usesFileSearch = !storeName.isBlank();

		try {
			if (usesFileSearch) {
				String groundedText = fileSearchClient.generateGroundedContent(resolvedModel,
						synapsysRequest.getSystemInstruction(), synapsysRequest.getContent(), storeName);
				return new LlmResponse(groundedText, TokenUsage.empty(), "gemini");
			}

			GenerateContentConfig config = buildSdkConfig(synapsysRequest.getSystemInstruction());
			GenerateContentResponse response = geminiSdkClient.models.generateContent(resolvedModel,
					synapsysRequest.getContent(), config);

			return new LlmResponse(response.text(), extractUsage(response), "gemini");

		} catch (ApiException exception) {
			int errorCode = exception.code();
			switch (errorCode) {
			case 403:
				throw new LlmProviderException(KEY, "Invalid API Key", exception);
			case 429:
				throw new LlmProviderException(RATE_LIMIT, "Gemini Resources Exhausted", exception);
			case 503:
				throw new LlmProviderException(UNAVAILABLE, "Gemini Service Unavailable", exception);
			default:
				throw new RuntimeException("Gemini Failure: " + exception.getMessage(), exception);
			}
		} catch (Exception exception) {
			throw new RuntimeException("Gemini Failure: " + exception.getMessage(), exception);
		}
	}

	private GenerateContentConfig buildSdkConfig(String systemInstructionText) {
		GenerateContentConfig.Builder builder = GenerateContentConfig.builder();

		if (systemInstructionText != null && !systemInstructionText.isBlank()) {
			Part sysPart = Part.builder().text(systemInstructionText).build();
			Content sysContent = Content.builder().parts(List.of(sysPart)).build();
			builder.systemInstruction(sysContent);
		}

		return builder.build();
	}

	private TokenUsage extractUsage(GenerateContentResponse response) {
		TokenUsage usage = TokenUsage.empty();
		var usageOpt = response.usageMetadata();
		if (usageOpt.isPresent()) {
			var um = usageOpt.get();
			int prompt = um.promptTokenCount().orElse(0);
			int candidates = um.candidatesTokenCount().orElse(0);
			int total = um.totalTokenCount().orElse(prompt + candidates);
			usage = new TokenUsage(prompt, candidates, total);
		}
		return usage;
	}

	private String readFileSearchStoreName(Map<String, Object> context) {
		Object raw = context.get(ContextKeys.FILE_SEARCH_STORE_NAME);
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
