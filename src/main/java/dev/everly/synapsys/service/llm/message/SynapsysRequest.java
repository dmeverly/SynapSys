package dev.everly.synapsys.service.llm.message;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SynapsysRequest extends Message {

	private final String llmProvider;
	private final String modelVersion;
	private final String systemInstruction;

	@JsonCreator
	public SynapsysRequest(@JsonProperty("sender") String sender, @JsonProperty("content") String content,
			@JsonProperty("context") Map<String, Object> context, @JsonProperty("llmProvider") String llmProvider,
			@JsonProperty("modelVersion") String modelVersion,
			@JsonProperty("systemInstruction") String systemInstruction) {
		super(sender, content, context);

		String normalizedProvider = Objects.requireNonNull(llmProvider, "llmProvider must not be null").trim();
		if (normalizedProvider.isEmpty()) {
			throw new IllegalArgumentException("llmProvider must not be blank");
		}

		this.llmProvider = normalizedProvider;
		this.modelVersion = Objects.requireNonNullElse(modelVersion, "").trim();
		this.systemInstruction = Objects.requireNonNullElse(systemInstruction, ""); // always non-null
	}

	public String getLlmProvider() {
		return llmProvider;
	}

	public String getModelVersion() {
		return modelVersion;
	}

	public String getSystemInstruction() {
		return systemInstruction;
	}
}
