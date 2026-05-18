package com.github.mattoyudzuru.terminalbang.app;

import com.github.mattoyudzuru.terminalbang.persistence.DatabaseSettings;

import java.nio.file.Path;

public record AppConfig(
        int sshPort,
        Path hostKeyPath,
        DatabaseSettings database
) {
    public static AppConfig fromEnvironment() {
        return new AppConfig(
                intEnv("TB_SSH_PORT", 2222),
                Path.of(env("TB_HOST_KEY_PATH", "data/hostkey.ser")),
                new DatabaseSettings(
                        env("TB_DB_URL", "jdbc:postgresql://localhost:5432/terminal_bang"),
                        env("TB_DB_USER", "terminal_bang"),
                        env("TB_DB_PASSWORD", "terminal_bang"),
                        intEnv("TB_DB_POOL_SIZE", 8)
                )
        );
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static int intEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}

