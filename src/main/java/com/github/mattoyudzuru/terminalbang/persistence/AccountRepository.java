package com.github.mattoyudzuru.terminalbang.persistence;

import com.github.mattoyudzuru.terminalbang.user.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Account findOrCreateByFingerprint(String sshFingerprint, String defaultNickname);

    Optional<Account> findById(UUID accountId);

    Account updateNickname(UUID accountId, String nickname);

    Account updateLanguage(UUID accountId, String language);
}
