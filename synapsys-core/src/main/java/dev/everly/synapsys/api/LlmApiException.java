package dev.everly.synapsys.api;
public class LlmApiException extends LlmClientException {
    private final int statusCode;
    private final String errorBody;
    public LlmApiException(String message, int statusCode, String errorBody) {
        super(message + " (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
        this.errorBody = errorBody;
    }
    public LlmApiException(String message, int statusCode, String errorBody, Throwable cause) {
        super(message + " (HTTP " + statusCode + ")", cause);
        this.statusCode = statusCode;
        this.errorBody = errorBody;
    }
    public int getStatusCode() {
        return statusCode;
    }
    public String getErrorBody() {
        return errorBody;
    }
}