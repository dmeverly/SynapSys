package dev.everly.synapsys.service.context;

import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import dev.everly.synapsys.service.sender.SenderConfig;

@Component
public final class CompositeLlmContextAugmenter implements LlmContextAugmenter {

	private final List<ProviderContextAugmenter> augmenters;

	public CompositeLlmContextAugmenter(List<ProviderContextAugmenter> augmenters) {
		this.augmenters = augmenters;
		AnnotationAwareOrderComparator.sort(this.augmenters);
	}

	@Override
	public Map<String, Object> augment(Map<String, Object> context, SenderConfig cfg) {
		Map<String, Object> current = context;
		for (ProviderContextAugmenter a : augmenters) {
			if (a.appliesTo(cfg.providerId())) {
				current = a.augment(current, cfg);
			}
		}
		return current;
	}
}
