package dev.everly.synapsys.service.strategy;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.everly.synapsys.service.llm.model.ApplicationMessage;
import dev.everly.synapsys.service.llm.model.SynapsysRequest;

@Component
@Order(Integer.MAX_VALUE)
public class DefaultSenderStrategy implements SenderStrategy {

	@Override
	public boolean appliesTo(String sender) {
		return true;
	}

	@Override
	public SynapsysRequest complete(ApplicationMessage inboundApplicationMessage) {
		String normalizedSender = inboundApplicationMessage.getSender().isBlank() ? "unknown"
				: inboundApplicationMessage.getSender();

		String llmProvider = "gemini";
		String modelVersion = "";
		String systemInstruction = "";

		return new SynapsysRequest(normalizedSender, inboundApplicationMessage.getContent(),
				inboundApplicationMessage.getContext(), llmProvider, modelVersion, systemInstruction);
	}
}
