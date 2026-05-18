package com.github.mattoyudzuru.terminalbang.user;

import java.time.Instant;
import java.util.UUID;

public record Account(
        UUID id,
        String sshFingerprint,
        String nickname,
        Instant createdAt,
        Instant updatedAt
) {
}

