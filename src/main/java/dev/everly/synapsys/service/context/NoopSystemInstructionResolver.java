package dev.everly.synapsys.service.context;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.everly.synapsys.service.llm.message.SynapsysRequest;

@Component
@Order(Integer.MAX_VALUE)
public class NoopSystemInstructionResolver implements SystemInstructionResolver {

	@Override
	public boolean appliesTo(String sender) {
		return true;
	}

	@Override
	public String resolve(SynapsysRequest request) {
		return "";
	}
}