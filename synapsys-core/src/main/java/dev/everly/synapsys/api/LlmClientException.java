package dev.everly.synapsys.api;
public class LlmClientException extends RuntimeException {
    public LlmClientException(String message) {
        super(message);
    }
    public LlmClientException(String message, Throwable cause) {
        super(message, cause);
    }
}