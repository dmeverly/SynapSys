package dev.everly.synapsys.service.llm;

public class LlmProviderException extends RuntimeException {

	private final Type type;

	public LlmProviderException(Type type, String message, Throwable cause) {
		super(message, cause);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public String getNeutralMessage() {
		return switch (type) {
		case RATE_LIMIT -> "I’m rate-limited right now. Please try again in a moment.";
		case UNAVAILABLE -> "The model provider is temporarily unavailable. Please try again shortly.";
		case KEY -> "Service is misconfigured (provider key). Please try again later.";
		case INVALID_REQUEST -> "That request couldn’t be processed. Please rephrase and try again.";
		default -> "Error communicating with the model. Please try again later.";
		};
	}

	public enum Type {
		RATE_LIMIT, UNAVAILABLE, INVALID_REQUEST, UNKNOWN, KEY
	}
}
