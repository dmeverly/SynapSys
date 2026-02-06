package dev.everly.synapsys.service.llm.message;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ApplicationMessage extends Message {

	@JsonCreator
	public ApplicationMessage(@JsonProperty("sender") String sender, @JsonProperty("content") String content,
			@JsonProperty("context") Map<String, Object> context) {
		super(sender, content, context);
	}
}
