package dev.everly.synapsys.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.everly.synapsys.service.guard.GuardViolationException;
import dev.everly.synapsys.service.llm.message.SynapsysResponse;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	private static String defaultUserMessage(String reasonCode) {
		if (reasonCode == null) {
			return "Request blocked by policy.";
		}
		return switch (reasonCode) {
		case "ILLEGAL_ACTIVITY" ->
			"I can't help with illegal activities. I can discuss defensive security best practices.";
		case "SYSTEM_PROMPT_REQUEST" -> "I can't share system instructions.";
		case "INJECTION_DETECTED" -> "I can’t help with attempts to override instructions or reveal internal data.";
		case "SECRETS_DETECTED" -> "I can’t process messages that include secrets. Remove them and try again.";
		case "INPUT_TOO_LARGE" -> "Your message is too long. Please shorten it and try again.";
		case "PROVIDER_TIMEOUT" -> "The upstream model timed out. Please try again.";
		case "SYSTEM_LEAKAGE" -> "I can't share internal instructions or hidden policies.";
		case "SENSITIVE_EGRESS" -> "I can’t share sensitive information.";
		case "OUTPUT_TOO_LARGE" -> "The response would be too long. Please narrow your question.";
		case "SECURITY_ADVICE", "SECURITY_CONTENT" -> "I can’t help with security-related requests.";
		case "OUT_OF_SCOPE" -> "I can help with questions about my portfolio, projects, and background.";
		case "MEDICAL_CONTENT" -> "I can’t help with medical requests.";
		case "PRIVATE_INFO" -> "I can’t help with requests for private personal information.";
		case "PII_PHI_INTAKE" ->
			"I can’t process messages containing personal or medical identifying information. Please redact it and try again.";
		default -> "Request blocked by policy.";
		};
	}

	private static String clientReason(String reasonCode) {
		if (reasonCode == null) {
			return "policy";
		}
		return switch (reasonCode) {
		case "INPUT_TOO_LARGE" -> "invalid_request";
		case "PROVIDER_TIMEOUT" -> "unavailable";
		default -> "policy";
		};
	}

	@ExceptionHandler(GuardViolationException.class)
	public ResponseEntity<SynapsysResponse> handleGuardViolation(GuardViolationException e) {
		String reasonCode = (e.getReasonCode() == null || e.getReasonCode().isBlank()) ? "policy" : e.getReasonCode();
		String userMsg = (e.getUserMessage() == null || e.getUserMessage().isBlank()) ? defaultUserMessage(reasonCode)
				: e.getUserMessage();

		Map<String, Object> logObj = new LinkedHashMap<>();
		logObj.put("event", "TX_BLOCKED");
		logObj.put("reasonCode", reasonCode);
		logObj.put("guard", e.getGuardId());
		logObj.put("sender", MDC.get("sender"));
		logObj.put("evidence", e.getEvidence());
		log.warn("<<< {}", logObj);

		return ResponseEntity.status(403).body(new SynapsysResponse("synapsys-guard", userMsg,
				Map.of("status", "blocked", "reason", clientReason(reasonCode))));
	}

	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<SynapsysResponse> handleSecurity(SecurityException e) {
		log.warn("<<< TX_BLOCKED | Policy Violation");
		return ResponseEntity.status(403).body(new SynapsysResponse("synapsys-guard", "Request blocked by policy.",
				Map.of("status", "blocked", "reason", "policy")));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<SynapsysResponse> handleBadArg(IllegalArgumentException e) {
		log.error("<<< TX_ERROR   | Invalid Input/Config: {}", e.getMessage());
		return ResponseEntity.status(400)
				.body(new SynapsysResponse("system", "Bad request.", Map.of("status", "error")));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<SynapsysResponse> handleGeneral(Exception e) {
		log.error("<<< TX_CRASH   | Unexpected: {}", e.getMessage(), e);
		return ResponseEntity.status(500)
				.body(new SynapsysResponse("system", "An internal server error occurred.", Map.of("status", "error")));
	}
}
