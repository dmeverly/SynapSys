package dev.everly.synapsys.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class TextCanon {

	private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200F\\uFEFF]");
	private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}\\p{Cf}]");
	private static final Pattern WS = Pattern.compile("\\s+");

	private TextCanon() {
	}

	public static String normalize(String s) {
		if (s == null) {
			return "";
		}
		String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
		n = ZERO_WIDTH.matcher(n).replaceAll("");
		n = CONTROL.matcher(n).replaceAll("");
		n = WS.matcher(n).replaceAll(" ").trim();
		return n;
	}
}
