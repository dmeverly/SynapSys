package dev.everly.synapsys.service.guard.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.everly.synapsys.service.guard.GuardEvidence;
import dev.everly.synapsys.service.guard.GuardViolationException;
import dev.everly.synapsys.service.guard.PreFlightGuard;
import dev.everly.synapsys.service.llm.message.SynapsysRequest;

@Component
@Order(10)
public class ResourceCapsGuard extends PreFlightGuard {

	private final int maxInputChars;

	public ResourceCapsGuard(@Value("${synapsys.limits.maxInputChars:4000}") int maxInputChars) {
		this.maxInputChars = maxInputChars;
	}

	@Override
	public void inspect(SynapsysRequest request) {
		String c = request.getContent();
		if (c == null || c.isBlank()) {
			Map<String, Object> evidence = new LinkedHashMap<>();
			evidence.put("category", "empty_input");
			throw new GuardViolationException("INVALID_REQUEST", "Bad request.", getClass().getSimpleName(), evidence);
		}
		if (c.length() > maxInputChars) {
			Map<String, Object> evidence = new LinkedHashMap<>();
			evidence.put("category", "input_too_large");
			evidence.put("length", c.length());
			evidence.put("max", maxInputChars);
			evidence.put("promptPreview", GuardEvidence.preview(c, 200));
			throw new GuardViolationException("INPUT_TOO_LARGE", "", getClass().getSimpleName(), evidence);
		}
	}
}
