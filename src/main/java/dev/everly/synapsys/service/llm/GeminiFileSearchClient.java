package dev.everly.synapsys.service.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.everly.synapsys.util.LogColor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class GeminiFileSearchClient {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String geminiApiKey;

	public GeminiFileSearchClient(HttpClient httpClient, ObjectMapper objectMapper, String geminiApiKey) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.geminiApiKey = geminiApiKey;
		log.warn(LogColor.warn("GeminiFileSearchClient Created: " + this));
	}

	public String generateGroundedContent(String modelName, String systemInstructionText, String userPromptText,
			String fileSearchStoreName) throws Exception {

		String endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName
				+ ":generateContent?key=" + geminiApiKey;

		ObjectNode root = objectMapper.createObjectNode();

		ArrayNode contentsArray = root.putArray("contents");
		ObjectNode userContentObject = contentsArray.addObject();
		userContentObject.put("role", "user");
		ArrayNode userPartsArray = userContentObject.putArray("parts");
		userPartsArray.addObject().put("text", userPromptText);

		if (systemInstructionText != null && !systemInstructionText.isBlank()) {
			ObjectNode systemInstructionObject = root.putObject("systemInstruction");
			ArrayNode systemInstructionPartsArray = systemInstructionObject.putArray("parts");
			systemInstructionPartsArray.addObject().put("text", systemInstructionText);
		}

		ArrayNode toolsArray = root.putArray("tools");
		ObjectNode toolObject = toolsArray.addObject();
		ObjectNode fileSearchObject = toolObject.putObject("fileSearch");
		ArrayNode storeNamesArray = fileSearchObject.putArray("fileSearchStoreNames");
		storeNamesArray.add(fileSearchStoreName);

		String requestBodyJson = objectMapper.writeValueAsString(root);

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpointUrl))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new RuntimeException(
					"Gemini FileSearch REST returned " + response.statusCode() + ": " + response.body());
		}

		JsonNode json = objectMapper.readTree(response.body());
		JsonNode textNode = json.at("/candidates/0/content/parts/0/text");
		return textNode.isMissingNode() ? "" : textNode.asText("");
	}
}
