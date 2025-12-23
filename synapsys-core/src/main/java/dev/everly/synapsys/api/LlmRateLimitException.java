package dev.everly.synapsys.api;
public class LlmRateLimitException extends LlmApiException {
    public LlmRateLimitException(String message, String errorBody) {
        super(message, 429, errorBody);
    }
    public LlmRateLimitException(String message, String errorBody, Throwable cause) {
        super(message, 429, errorBody, cause);
    }
}