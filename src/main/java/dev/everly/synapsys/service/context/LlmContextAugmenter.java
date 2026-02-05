package dev.everly.synapsys.service.context;

import java.util.Map;

import dev.everly.synapsys.service.sender.SenderConfig;

public interface LlmContextAugmenter {
	Map<String, Object> augment(Map<String, Object> context, SenderConfig cfg);
}
