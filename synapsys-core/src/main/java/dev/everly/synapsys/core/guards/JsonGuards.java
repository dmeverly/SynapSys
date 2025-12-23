package dev.everly.synapsys.core.guards;

import dev.everly.synapsys.core.util.JsonUtils;

public final class JsonGuards {
	private JsonGuards() {
	}

	public static String[] candidates(String raw) {
		String firstObject = JsonUtils.extractFirstJsonObject(raw);
		return new String[] { raw, JsonUtils.stripCodeFences(raw), firstObject,
				JsonUtils.normalizeModelJson(firstObject) };
	}
}
