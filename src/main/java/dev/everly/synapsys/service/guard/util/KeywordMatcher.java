package dev.everly.synapsys.service.guard.util;

import java.util.List;

public class KeywordMatcher {
	private final List<String> keywords;

	public KeywordMatcher(List<String> keywords) {
		this.keywords = keywords.stream().map(String::toLowerCase).toList();
	}

	public boolean matchesAnyKeyword(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String lowerText = text.toLowerCase();
		return keywords.stream().anyMatch(lowerText::contains);
	}
}