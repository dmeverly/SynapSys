package dev.everly.synapsys.util;

public final class LogColor {

	public static final String RESET = "\u001B[0m";
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String YELLOW = "\u001B[33m";
	public static final String BOLD = "\u001B[1m";

	private LogColor() {
	}

	public static String live(String msg) {
		return RED + BOLD + "[LIVE] " + msg + RESET;
	}

	public static String test(String msg) {
		return GREEN + BOLD + "[TEST] " + msg + RESET;
	}

	public static String warn(String msg) {
		return YELLOW + BOLD + "[WARN] " + msg + RESET;
	}
}
