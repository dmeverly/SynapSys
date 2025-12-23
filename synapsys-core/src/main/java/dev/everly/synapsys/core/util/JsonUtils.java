package dev.everly.synapsys.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtils {
	private static final Pattern FENCE = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");
	private static final Pattern FIRST_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
	private JsonUtils() {
	}

	public static String stripCodeFences(String text) {
		if (text == null) {
			return null;
		}
		Matcher m = FENCE.matcher(text);
		return m.find() ? m.group(1) : text;
	}

	public static String extractFirstJsonObject(String text) {
		if (text == null) {
			return null;
		}
		Matcher m = FIRST_OBJECT.matcher(text);
		return m.find() ? m.group() : text;
	}

	public static String normalizeModelJson(String text) {
		if (text == null) {
			return null;
		}
		String s = text.replace('\u201c', '"').replace('\u201d', '"') // smart quotes
				.replace('\u2018', '\'').replace('\u2019', '\'');

		s = s.replaceAll(",\\s*([}\\]])", "$1");
		return s;
	}
}
