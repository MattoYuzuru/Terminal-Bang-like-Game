package com.github.mattoyudzuru.terminalbang.user;

import java.time.Instant;
import java.util.UUID;

public record Account(
        UUID id,
        String sshFingerprint,
        String nickname,
        String language,
        Instant createdAt,
        Instant updatedAt
) {
    public Account(UUID id, String sshFingerprint, String nickname, Instant createdAt, Instant updatedAt) {
        this(id, sshFingerprint, nickname, "en", createdAt, updatedAt);
    }
}
