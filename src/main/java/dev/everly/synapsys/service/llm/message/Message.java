package dev.everly.synapsys.service.llm.message;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public abstract class Message {

	private final String sender;
	private final String content;
	private final Map<String, Object> context;
	private final Instant timestamp;

	@JsonCreator
	protected Message(@JsonProperty("sender") String sender, @JsonProperty("content") String content,
			@JsonProperty("context") Map<String, Object> context) {
		this.sender = requireNonBlank(sender, "sender");
		this.content = requireNonBlank(content, "content");
		this.context = (context == null) ? Map.of() : Map.copyOf(context);
		this.timestamp = Instant.now();
	}

	private static String requireNonBlank(String value, String field) {
		String trimmed = Objects.requireNonNull(value, field + " must not be null").trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return trimmed;
	}

}
