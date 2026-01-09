package dev.everly.synapsys.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.everly.synapsys.service.llm.model.SynapsysResponse;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<SynapsysResponse> handleSecurity(SecurityException e) {
		log.warn("<<< TX_BLOCKED | Policy Violation: {}", safeMsg(e));

		return ResponseEntity.status(403).body(new SynapsysResponse(
				"synapsys-guard",
				"Request blocked by policy.",
				Map.of("status", "blocked", "reason", "policy")));

	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<SynapsysResponse> handleBadArg(IllegalArgumentException e) {
		log.error("<<< TX_ERROR   | Invalid Input/Config: {}", safeMsg(e));

		return ResponseEntity.status(400).body(new SynapsysResponse(
				"system",
				"Bad request.",
				Map.of("status", "error")));

	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<SynapsysResponse> handleGeneral(Exception e) {
		log.error("<<< TX_CRASH   | Unexpected: {}", safeMsg(e), e);

		return ResponseEntity.status(500).body(new SynapsysResponse("system",
				"An internal server error occurred.", Map.of("status", "error")));
	}

	private static String safeMsg(Throwable t) {
		if (t == null || t.getMessage() == null)
			return "";

		String s = t.getMessage().replace("\r", "\\r").replace("\n", "\\n");
		return s.length() > 300 ? s.substring(0, 300) + "…" : s;
	}

}