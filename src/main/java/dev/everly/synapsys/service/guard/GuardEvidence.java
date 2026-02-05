package dev.everly.synapsys.service.guard;

public final class GuardEvidence {

	private GuardEvidence() {
	}

	public static String preview(String s, int max) {
		if (s == null) {
			return "";
		}
		String t = s.replace("\r", "\\r").replace("\n", "\\n");
		return t.length() > max ? t.substring(0, max) + "…" : t;
	}
}
