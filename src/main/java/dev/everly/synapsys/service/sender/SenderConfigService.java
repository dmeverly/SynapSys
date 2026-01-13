package dev.everly.synapsys.service.sender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.everly.synapsys.service.secrets.SecretsLocator;

@Service
public final class SenderConfigService {

	private final SecretsLocator secretsLocator;
	private final ObjectMapper mapper;
	private final Map<String, SenderConfig> cache = new ConcurrentHashMap<>();

	public SenderConfigService(SecretsLocator secretsLocator, ObjectMapper mapper) {
		this.secretsLocator = secretsLocator;
		this.mapper = mapper;
	}

	private static SenderConfig validate(SenderConfig cfg, String source) {
		requireNonBlank(cfg.senderId(), "senderId", source);
		requireNonBlank(cfg.synapsysClientKey(), "synapsysClientKey", source);
		requireNonBlank(cfg.providerId(), "providerId", source);

		return cfg;
	}

	private static void requireNonBlank(String value, String field, String source) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing/blank '" + field + "' in " + source);
		}
	}

	private static String normalize(String senderId) {
		if (senderId == null || senderId.isBlank()) {
			throw new IllegalArgumentException("senderId is required");
		}
		return senderId.trim().toLowerCase(Locale.ROOT);
	}

	public boolean hasConfig(String senderId) {
		try {
			String key = normalize(senderId);
			if (cache.containsKey(key)) {
				return true;
			}
			Path path = secretsLocator.baseDir().resolve("senders").resolve(key + ".json").normalize();
			return Files.exists(path);
		} catch (Exception e) {
			return false;
		}
	}

	public SenderConfig getRequired(String senderId) {
		String key = normalize(senderId);
		return cache.computeIfAbsent(key, this::loadAndValidate);
	}

	private SenderConfig loadAndValidate(String normalizedSenderId) {
		Path path = secretsLocator.baseDir().resolve("senders").resolve(normalizedSenderId + ".json").normalize();

		try {
			String json = Files.readString(path);
			SenderConfig cfg = mapper.readValue(json, SenderConfig.class);
			return validate(cfg, path.toString());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load sender config: " + path, e);
		}
	}
}
