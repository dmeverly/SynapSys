package dev.everly.synapsys.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.everly.synapsys.service.sender.SenderConfig;
import dev.everly.synapsys.service.sender.SenderConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	private static final String HEADER_KEY = "X-SynapSys-Key";
	private static final String HEADER_SENDER = "X-SynapSys-Sender";
	private static final Pattern SENDER_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$", Pattern.CASE_INSENSITIVE);

	private static final Set<String> RESERVED_SENDERS = Set.of("anonymous", "system", "synapsys", "test", "admin",
			"root");

	private final ObjectMapper objectMapper;
	private final SenderConfigService senderConfigService;

	public ApiKeyAuthFilter(ObjectMapper objectMapper, SenderConfigService senderConfigService) {
		this.objectMapper = objectMapper;
		this.senderConfigService = senderConfigService;
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null || b.isBlank()) {
			return false;
		}
		byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
		byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(aBytes, bBytes);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String requestKey = request.getHeader(HEADER_KEY);
		String senderHeaderValue = request.getHeader(HEADER_SENDER);

		if (senderHeaderValue == null || senderHeaderValue.isBlank()) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED,
					"Authentication required. Please provide an X-SynapSys-Sender header.", "missing_sender");
			return;
		}

		String trimmedSender = senderHeaderValue.trim();
		String normalizedSender = trimmedSender.toLowerCase();

		if (!SENDER_PATTERN.matcher(trimmedSender).matches()) {
			writeDenialResponse(response, HttpStatus.BAD_REQUEST,
					"Sender name must contain only alphanumeric characters, hyphens, and underscores (1-64 chars).",
					"invalid_sender_format");
			return;
		}

		if (RESERVED_SENDERS.contains(normalizedSender)) {
			writeDenialResponse(response, HttpStatus.FORBIDDEN, "The sender name '" + trimmedSender
					+ "' is reserved for system use. Please use a different identifier.", "reserved_sender");
			return;
		}

		SenderConfig cfg;
		try {
			cfg = senderConfigService.getRequired(normalizedSender);
		} catch (Exception e) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED, "Authentication required. Unknown sender.",
					"unknown_sender");
			return;
		}

		String expectedKey = cfg.synapsysClientKey();

		if (!constantTimeEquals(requestKey, expectedKey)) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED,
					"Authentication required. Please provide a valid X-SynapSys-Key header.", "invalid_api_key");
			return;
		}

		var auth = new UsernamePasswordAuthenticationToken(trimmedSender, null, Collections.emptyList());
		SecurityContextHolder.getContext().setAuthentication(auth);
		filterChain.doFilter(request, response);
	}

	private void writeDenialResponse(HttpServletResponse response, HttpStatus status, String message, String reason)
			throws IOException {

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Map<String, Object> body = new HashMap<>();
		body.put("sender", "synapsys-guard");
		body.put("content", message);

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("status", "blocked");
		metadata.put("reason", reason);
		body.put("metadata", metadata);

		body.put("context", Collections.emptyMap());
		body.put("timestamp", Instant.now().toString());

		objectMapper.writeValue(response.getWriter(), body);
	}
}
