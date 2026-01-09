package dev.everly.synapsys.service.sender;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SenderConfig(String senderId, String synapsysClientKey, String providerId, String model,
		String systemInstructionPath, String fileSearchStoreName) {
}
