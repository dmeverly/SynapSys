package dev.everly.synapsys.service.strategy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.everly.synapsys.service.context.LlmContextAugmenter;
import dev.everly.synapsys.service.llm.model.ApplicationMessage;
import dev.everly.synapsys.service.llm.model.SynapsysRequest;
import dev.everly.synapsys.service.sender.SenderConfig;
import dev.everly.synapsys.service.sender.SenderConfigService;

@Component
@Order(0)
public final class RegistrySenderStrategy implements SenderStrategy {

	private final SenderConfigService senderConfigService;
	private final LlmContextAugmenter contextAugmenter;

	public RegistrySenderStrategy(SenderConfigService senderConfigService, LlmContextAugmenter contextAugmenter) {
		this.senderConfigService = senderConfigService;
		this.contextAugmenter = contextAugmenter;
	}

	@Override
	public boolean appliesTo(String sender) {
		return sender != null && !sender.isBlank() && senderConfigService.hasConfig(sender);
	}

	@Override
	public SynapsysRequest complete(ApplicationMessage inboundMessage) {
		SenderConfig cfg = senderConfigService.getRequired(inboundMessage.getSender());

		Map<String, Object> ctx = new LinkedHashMap<>(inboundMessage.getContext());
		ctx = contextAugmenter.augment(ctx, cfg);

		return new SynapsysRequest(inboundMessage.getSender(), inboundMessage.getContent(), ctx, cfg.providerId(),
				cfg.model() == null ? "" : cfg.model().trim(), "");
	}
}
