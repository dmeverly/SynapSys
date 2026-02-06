package dev.everly.synapsys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.everly.synapsys.service.llm.GeminiFileSearchClient;

@Configuration
public class AppConfig {

	@Bean
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

	@Bean
	public HttpClient httpClient() {
		return HttpClient.newHttpClient();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public GeminiFileSearchClient geminiFileSearchClient(HttpClient httpClient, ObjectMapper objectMapper,
			LlmConfig config) {
		String apiKey = (config.llm() != null && config.llm().geminiKey() != null) ? config.llm().geminiKey().trim()
				: "";
		return new GeminiFileSearchClient(httpClient, objectMapper, apiKey);
	}
}