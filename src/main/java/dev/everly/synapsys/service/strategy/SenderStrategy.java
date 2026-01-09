package dev.everly.synapsys.service.strategy;

import dev.everly.synapsys.service.llm.model.ApplicationMessage;
import dev.everly.synapsys.service.llm.model.SynapsysRequest;

public interface SenderStrategy {
	boolean appliesTo(String sender);

	SynapsysRequest complete(ApplicationMessage inboundApplicationMessage);
}