package dev.everly.synapsys.service.llm.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LlmResponse extends Message {

	private final Map<String, Object> metadata;

	@JsonCreator
	public LlmResponse(@JsonProperty("sender") String sender, @JsonProperty("content") String content,
			@JsonProperty("metadata") Map<String, Object> metadata) {
		super(sender, content, metadata);
		this.metadata = metadata;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}
}
