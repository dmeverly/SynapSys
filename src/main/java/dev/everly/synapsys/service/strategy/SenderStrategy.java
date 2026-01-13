package dev.everly.synapsys.service.strategy;

import dev.everly.synapsys.service.llm.message.ApplicationMessage;
import dev.everly.synapsys.service.llm.message.SynapsysRequest;

public interface SenderStrategy {
	boolean appliesTo(String sender);

	SynapsysRequest complete(ApplicationMessage inboundApplicationMessage);
}