package dev.everly.synapsys.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import lombok.extern.slf4j.Slf4j;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final long MAX_SKEW_SECONDS = 60;

    private final ObjectMapper objectMapper;
    private final SenderConfigService senderConfigService;
    private final NonceCache nonceCache;

    private final SenderPolicy senderPolicy = SenderPolicy.defaultPolicy();
    private final RequestCanonicalizer canonicalizer = new RequestCanonicalizer();
    private final SignatureVerifier signatureVerifier = new SignatureVerifier();

    public ApiKeyAuthFilter(ObjectMapper objectMapper, SenderConfigService senderConfigService, NonceCache nonceCache) {
        this.objectMapper = objectMapper;
        this.senderConfigService = senderConfigService;
        this.nonceCache = nonceCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            SignedRequestHeaders headers = SignedRequestHeaders.from(request);

            senderPolicy.validateSender(headers.senderOriginal(), headers.senderNormalized());

            headers.validateSignatureHeadersPresent();

            long requestEpochSeconds = headers.parseTimestampEpochSeconds();
            enforceTimestampWindow(requestEpochSeconds);

            enforceNonceFreshness(headers.senderNormalized(), headers.nonce());

            SenderConfig senderConfig = loadSenderConfig(headers.senderNormalized());

            String senderSecret = requireSenderSecret(senderConfig);

            byte[] requestBodyBytes = readCachedBodyBytes(request);

            String canonicalString = canonicalizer.buildCanonicalV1(
                    request.getMethod(),
                    canonicalizer.pathWithQuery(request),
                    headers.senderOriginal(),
                    headers.timestampRaw(),
                    headers.nonce(),
                    canonicalizer.sha256Hex(requestBodyBytes)
            );

            signatureVerifier.verifyOrThrow(headers.signatureRaw(), senderSecret, canonicalString);

            establishAuthentication(headers.senderOriginal());
            filterChain.doFilter(request, response);

        } catch (AuthFailureException authFailure) {
            writeDenialResponse(response, authFailure.httpStatus, authFailure.userMessage, authFailure.reasonCode);
        }
    }

    private void enforceTimestampWindow(long requestEpochSeconds) {
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - requestEpochSeconds) > MAX_SKEW_SECONDS) {
            throw AuthFailureException.unauthorized(
                    "Authentication required. Request timestamp outside allowed window.",
                    "timestamp_out_of_window"
            );
        }
    }

    private void enforceNonceFreshness(String normalizedSender, String nonce) {
        if (!nonceCache.markIfNew(normalizedSender, nonce)) {
            throw AuthFailureException.unauthorized(
                    "Authentication required. Replay detected.",
                    "replay_detected"
            );
        }
    }

    private SenderConfig loadSenderConfig(String normalizedSender) {
        try {
            return senderConfigService.getRequired(normalizedSender);
        } catch (Exception e) {
            log.warn("Sender config lookup failed for '{}': {}", normalizedSender, e.toString());
            throw AuthFailureException.unauthorized(
                    "Authentication required. Unknown sender.",
                    "unknown_sender"
            );
        }
    }

    private String requireSenderSecret(SenderConfig cfg) {
        String secret = cfg.synapsysClientKey();
        if (secret == null || secret.isBlank()) {
            throw AuthFailureException.unauthorized(
                    "Authentication required. Sender not configured.",
                    "sender_not_configured"
            );
        }
        return secret;
    }

    private byte[] readCachedBodyBytes(HttpServletRequest request) {
        if (request instanceof CachedBodyHttpServletRequest wrapped) {
            return wrapped.getCachedBody();
        }
        // If CachedBodyFilter isn't applied correctly, fail closed.
        return new byte[0];
    }

    private void establishAuthentication(String sender) {
        var auth = new UsernamePasswordAuthenticationToken(sender, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
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

    static final class SignedRequestHeaders {
        private static final String HEADER_SENDER = "X-SynapSys-Sender";
        private static final String HEADER_TS = "X-SynapSys-Timestamp";
        private static final String HEADER_NONCE = "X-SynapSys-Nonce";
        private static final String HEADER_SIG = "X-SynapSys-Signature";

        private final String senderOriginal;
        private final String senderNormalized;
        private final String timestampRaw;
        private final String nonce;
        private final String signatureRaw;

        private SignedRequestHeaders(String senderOriginal, String senderNormalized, String timestampRaw, String nonce,
                String signatureRaw) {
            this.senderOriginal = senderOriginal;
            this.senderNormalized = senderNormalized;
            this.timestampRaw = timestampRaw;
            this.nonce = nonce;
            this.signatureRaw = signatureRaw;
        }

        static SignedRequestHeaders from(HttpServletRequest request) {
            String senderHeader = request.getHeader(HEADER_SENDER);
            if (senderHeader == null || senderHeader.isBlank()) {
                throw AuthFailureException.unauthorized(
                        "Authentication required. Please provide an X-SynapSys-Sender header.",
                        "missing_sender"
                );
            }

            String senderTrimmed = senderHeader.trim();
            String normalized = senderTrimmed.toLowerCase(Locale.ROOT);

            String ts = valueOrEmpty(request.getHeader(HEADER_TS));
            String nonce = valueOrEmpty(request.getHeader(HEADER_NONCE));
            String sig = valueOrEmpty(request.getHeader(HEADER_SIG));

            return new SignedRequestHeaders(senderTrimmed, normalized, ts, nonce, sig);
        }

        void validateSignatureHeadersPresent() {
            if (timestampRaw.isBlank() || nonce.isBlank() || signatureRaw.isBlank()) {
                throw AuthFailureException.unauthorized(
                        "Authentication required. Missing signature headers.",
                        "missing_signature_headers"
                );
            }
        }

        long parseTimestampEpochSeconds() {
            try {
                return Long.parseLong(timestampRaw.trim());
            } catch (NumberFormatException e) {
                throw AuthFailureException.badRequest("Invalid timestamp header.", "invalid_timestamp");
            }
        }

        String senderOriginal() {
            return senderOriginal;
        }

        String senderNormalized() {
            return senderNormalized;
        }

        String timestampRaw() {
            return timestampRaw;
        }

        String nonce() {
            return nonce.trim();
        }

        String signatureRaw() {
            return signatureRaw;
        }

        private static String valueOrEmpty(String s) {
            return s == null ? "" : s.trim();
        }
    }

    static final class SenderPolicy {
        private static final Pattern SENDER_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$",
                Pattern.CASE_INSENSITIVE);

        private final Set<String> reservedSenders;

        private SenderPolicy(Set<String> reservedSenders) {
            this.reservedSenders = reservedSenders;
        }

        static SenderPolicy defaultPolicy() {
            return new SenderPolicy(Set.of("anonymous", "system", "synapsys", "test", "admin", "root"));
        }

        void validateSender(String senderOriginal, String senderNormalized) {
            if (!SENDER_PATTERN.matcher(senderOriginal).matches()) {
                throw AuthFailureException.badRequest(
                        "Sender name must contain only alphanumeric characters, hyphens, and underscores (1-64 chars).",
                        "invalid_sender_format"
                );
            }

            if (reservedSenders.contains(senderNormalized)) {
                throw AuthFailureException.forbidden(
                        "The sender name '" + senderOriginal + "' is reserved for system use. Please use a different identifier.",
                        "reserved_sender"
                );
            }
        }
    }

    static final class RequestCanonicalizer {

        String pathWithQuery(HttpServletRequest request) {
            String path = request.getRequestURI();
            String query = request.getQueryString();
            if (query != null && !query.isBlank()) {
                return path + "?" + query;
            }
            return path;
        }

        String buildCanonicalV1(String method, String pathWithQuery, String sender, String timestamp, String nonce,
                String bodySha256Hex) {
            return String.join("\n",
                    "v1",
                    method.toUpperCase(),
                    pathWithQuery,
                    sender,
                    timestamp,
                    nonce,
                    bodySha256Hex
            );
        }

        String sha256Hex(byte[] data) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(data);
                StringBuilder sb = new StringBuilder(digest.length * 2);
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                throw AuthFailureException.serverError("Unable to validate request.", "hash_failure", e);
            }
        }
    }

    static final class SignatureVerifier {

        void verifyOrThrow(String providedSignature, String secret, String canonical) {
            String expected = hmacBase64(secret.getBytes(StandardCharsets.UTF_8), canonical);

            if (!constantTimeEquals(providedSignature.trim(), expected)) {
                throw AuthFailureException.unauthorized(
                        "Authentication required. Invalid signature.",
                        "invalid_signature"
                );
            }
        }

        private static String hmacBase64(byte[] secret, String data) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret, "HmacSHA256"));
                byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(out);
            } catch (Exception e) {
                throw AuthFailureException.serverError("Unable to validate request.", "hmac_failure", e);
            }
        }

        private static boolean constantTimeEquals(String a, String b) {
            if (a == null || b == null || a.isBlank() || b.isBlank()) {
                return false;
            }
            return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
        }
    }

    static final class AuthFailureException extends RuntimeException {
        final HttpStatus httpStatus;
        final String userMessage;
        final String reasonCode;

        private AuthFailureException(HttpStatus httpStatus, String userMessage, String reasonCode, Throwable cause) {
            super(userMessage, cause);
            this.httpStatus = httpStatus;
            this.userMessage = userMessage;
            this.reasonCode = reasonCode;
        }

        static AuthFailureException unauthorized(String message, String reason) {
            return new AuthFailureException(HttpStatus.UNAUTHORIZED, message, reason, null);
        }

        static AuthFailureException forbidden(String message, String reason) {
            return new AuthFailureException(HttpStatus.FORBIDDEN, message, reason, null);
        }

        static AuthFailureException badRequest(String message, String reason) {
            return new AuthFailureException(HttpStatus.BAD_REQUEST, message, reason, null);
        }

        static AuthFailureException serverError(String message, String reason, Throwable cause) {
            return new AuthFailureException(HttpStatus.INTERNAL_SERVER_ERROR, message, reason, cause);
        }
    }
}
