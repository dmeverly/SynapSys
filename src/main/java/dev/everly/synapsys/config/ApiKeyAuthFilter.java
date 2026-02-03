package dev.everly.synapsys.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.everly.synapsys.authentication.CachedBodyHttpServletRequest;
import dev.everly.synapsys.authentication.NonceCache;
import dev.everly.synapsys.service.sender.SenderConfig;
import dev.everly.synapsys.service.sender.SenderConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	private static final Pattern SENDER_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$", Pattern.CASE_INSENSITIVE);

	private static final String HEADER_SENDER = "X-SynapSys-Sender";
	private static final String HEADER_TS = "X-SynapSys-Timestamp";
	private static final String HEADER_NONCE = "X-SynapSys-Nonce";
	private static final String HEADER_SIG = "X-SynapSys-Signature";

	private static final long MAX_SKEW_SECONDS = 60;

	private static final Set<String> RESERVED_SENDERS = Set.of("anonymous", "system", "synapsys", "test", "admin",
			"root");

	private final ObjectMapper objectMapper;
	private final SenderConfigService senderConfigService;
	private final NonceCache nonceCache;

	public ApiKeyAuthFilter(ObjectMapper objectMapper, SenderConfigService senderConfigService, NonceCache nonceCache) {
		this.objectMapper = objectMapper;
		this.senderConfigService = senderConfigService;
		this.nonceCache = nonceCache;
	}

	private static String sha256Hex(byte[] data) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] digest = md.digest(data);
		StringBuilder sb = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static String hmacBase64(byte[] secret, String data) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret, "HmacSHA256"));
		byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(out);
	}

	private static String canonicalV1(String method, String pathWithQuery, String sender, String timestamp,
			String nonce, String bodySha256Hex) {
		return String.join("\n", "v1", method.toUpperCase(), pathWithQuery, sender, timestamp, nonce, bodySha256Hex);
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null || a.isBlank() || b.isBlank()) {
			return false;
		}
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String tsHeader = request.getHeader(HEADER_TS);
		String nonceHeader = request.getHeader(HEADER_NONCE);
		String sigHeader = request.getHeader(HEADER_SIG);

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

		if (tsHeader == null || tsHeader.isBlank() || nonceHeader == null || nonceHeader.isBlank() || sigHeader == null
				|| sigHeader.isBlank()) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED,
					"Authentication required. Missing signature headers.", "missing_signature_headers");
			return;
		}

		long ts;
		try {
			ts = Long.parseLong(tsHeader.trim());
		} catch (NumberFormatException e) {
			writeDenialResponse(response, HttpStatus.BAD_REQUEST, "Invalid timestamp header.", "invalid_timestamp");
			return;
		}

		long now = Instant.now().getEpochSecond();
		if (Math.abs(now - ts) > MAX_SKEW_SECONDS) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED,
					"Authentication required. Request timestamp outside allowed window.", "timestamp_out_of_window");
			return;
		}

		String nonce = nonceHeader.trim();
		if (!nonceCache.markIfNew(normalizedSender, nonce)) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED, "Authentication required. Replay detected.",
					"replay_detected");
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

		String secret = cfg.synapsysClientKey();
		if (secret == null || secret.isBlank()) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED, "Authentication required. Sender not configured.",
					"sender_not_configured");
			return;
		}

		byte[] bodyBytes = new byte[0];
		if (request instanceof CachedBodyHttpServletRequest wrapped) {
			bodyBytes = wrapped.getCachedBody();
		}

		final String bodyHashHex;
		try {
			bodyHashHex = sha256Hex(bodyBytes);
		} catch (Exception e) {
			writeDenialResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to validate request.",
					"hash_failure");
			return;
		}

		String pathWithQuery = request.getRequestURI();
		if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
			pathWithQuery += "?" + request.getQueryString();
		}

		final String canonical = canonicalV1(request.getMethod(), pathWithQuery, trimmedSender, tsHeader.trim(), nonce,
				bodyHashHex);

		final String expectedSig;
		try {
			expectedSig = hmacBase64(secret.getBytes(StandardCharsets.UTF_8), canonical);
		} catch (Exception e) {
			writeDenialResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to validate request.",
					"hmac_failure");
			return;
		}

		if (!constantTimeEquals(sigHeader.trim(), expectedSig)) {
			writeDenialResponse(response, HttpStatus.UNAUTHORIZED, "Authentication required. Invalid signature.",
					"invalid_signature");
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
