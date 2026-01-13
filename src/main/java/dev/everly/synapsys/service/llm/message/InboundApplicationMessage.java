package dev.everly.synapsys.service.llm.message;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class InboundApplicationMessage {

	private final String content;
	private final Map<String, Object> context;

	@JsonCreator
	public InboundApplicationMessage(@JsonProperty("content") String content,
			@JsonProperty("context") Map<String, Object> context) {
		this.content = content;
		this.context = (context == null) ? Map.of() : context;
	}

	public String getContent() {
		return content;
	}

	public Map<String, Object> getContext() {
		return context;
	}
}
