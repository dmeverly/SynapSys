package dev.everly.synapsys.service.context;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.everly.synapsys.service.llm.ContextKeys;
import dev.everly.synapsys.service.sender.SenderConfig;

@Component
@Order(0)
public final class GeminiFileSearchStoreAugmenter implements ProviderContextAugmenter {

	@Override
	public boolean appliesTo(String providerId) {
		return "gemini".equalsIgnoreCase(providerId);
	}

	@Override
	public Map<String, Object> augment(Map<String, Object> context, SenderConfig cfg) {
		String store = cfg.fileSearchStoreName();
		if (store == null || store.isBlank()) {
			return context;
		}

		Map<String, Object> out = new LinkedHashMap<>(context);
		out.put(ContextKeys.FILE_SEARCH_STORE_NAME, store.trim());
		return out;
	}
}
