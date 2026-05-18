package com.github.mattoyudzuru.terminalbang.ssh;

import org.apache.sshd.server.Environment;

import java.util.Map;

public record TerminalSize(int columns, int rows) {
    public static final int MIN_COLUMNS = 90;
    public static final int MIN_ROWS = 30;

    public static TerminalSize from(Environment environment) {
        Map<String, String> env = environment.getEnv();
        return new TerminalSize(
                parse(env.get("COLUMNS"), 120),
                parse(env.get("LINES"), 40)
        );
    }

    public boolean tooSmall() {
        return columns < MIN_COLUMNS || rows < MIN_ROWS;
    }

    private static int parse(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}

