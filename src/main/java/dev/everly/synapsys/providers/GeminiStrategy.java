package dev.everly.synapsys.providers;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class GeminiStrategy extends ProviderStrategy {
    private final HttpClient httpClient;

    public GeminiStrategy(String provider, String model, String api_key, String query) {
        super(provider, model, api_key, query);
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String generate() {
        try {
            String json = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(this.getQuery()) + "\"}]}]}";

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(buildEndpointString()))
                    .header("Content-Type", "application/json");

            String key = getApiKey();
            reqBuilder.header("x-goog-api-key", key);

            HttpRequest request = reqBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String buildEndpointString() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + getModel() + ":generateContent";
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
