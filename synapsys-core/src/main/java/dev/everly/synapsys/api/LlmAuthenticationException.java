package dev.everly.synapsys.api;
public class LlmAuthenticationException extends LlmApiException {
    public LlmAuthenticationException(String message, String errorBody) {
        super(message, 401, errorBody);
    }
    public LlmAuthenticationException(String message, String errorBody, Throwable cause) {
        super(message, 401, errorBody, cause);
    }
}