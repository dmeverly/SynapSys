package dev.everly.synapsys.service.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.everly.synapsys.service.llm.model.SynapsysRequest;
import dev.everly.synapsys.service.secrets.SecretsLocator;
import dev.everly.synapsys.service.sender.SenderConfig;
import dev.everly.synapsys.service.sender.SenderConfigService;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(0)
@Slf4j
public final class RegistrySystemInstructionResolver implements SystemInstructionResolver {

	private final SenderConfigService senderConfigService;
	private final SecretsLocator secretsLocator;
	private final Map<String, String> instructionCache = new ConcurrentHashMap<>();

	public RegistrySystemInstructionResolver(SenderConfigService senderConfigService, SecretsLocator secretsLocator) {
		this.senderConfigService = senderConfigService;
		this.secretsLocator = secretsLocator;
	}

	private static String normalize(String senderId) {
		return senderId.trim().toLowerCase(Locale.ROOT);
	}

	private static String sha256Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (Exception e) {
			return "unavailable";
		}
	}

	@Override
	public boolean appliesTo(String sender) {
		return sender != null && !sender.isBlank();
	}

	@Override
	public String resolve(SynapsysRequest request) {
		String senderId = normalize(request.getSender());
		SenderConfig cfg = senderConfigService.getRequired(senderId);

		String rel = cfg.systemInstructionPath();
		if (rel == null || rel.isBlank()) {
			return "";
		}

		return instructionCache.computeIfAbsent(senderId, _ignored -> readInstruction(rel));
	}

	private String readInstruction(String relativePath) {
		Path path = secretsLocator.baseDir().resolve(relativePath).normalize();

		try {
			String text = Files.readString(path);

			log.info("Loaded system instruction file: path={} bytes={} sha256={}", path, text.length(),
					sha256Hex(text));

			return text;

		} catch (IOException e) {
			throw new IllegalStateException("Failed to read system instruction file: " + path, e);
		}
	}
}
