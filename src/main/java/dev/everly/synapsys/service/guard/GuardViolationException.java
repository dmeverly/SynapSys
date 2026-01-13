package dev.everly.synapsys.service.guard;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

@Getter
public class GuardViolationException extends SecurityException {

	private final String reasonCode;
	private final String userMessage;
	private final String guardId;
	private final Map<String, Object> evidence;

	public GuardViolationException(String reasonCode, String userMessage, String guardId,
			Map<String, Object> evidence) {
		super(reasonCode);
		this.reasonCode = Objects.requireNonNullElse(reasonCode, "policy");
		this.userMessage = Objects.requireNonNullElse(userMessage, "");
		this.guardId = Objects.requireNonNullElse(guardId, "unknown_guard");
		this.evidence = (evidence == null) ? Collections.emptyMap() : Collections.unmodifiableMap(evidence);
	}

}
