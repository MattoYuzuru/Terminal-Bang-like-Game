package com.github.mattoyudzuru.terminalbang.ssh;

import com.github.mattoyudzuru.terminalbang.tui.TerminalSession;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SshGameServer implements Closeable {
    private final SshServer sshServer;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<Long, LoginIdentity> identities = new ConcurrentHashMap<>();

    public SshGameServer(int port, Path hostKeyPath, TerminalSession terminalSession) {
        this.sshServer = SshServer.setUpDefaultServer();
        this.sshServer.setPort(port);
        ensureParentDirectory(hostKeyPath);
        this.sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));
        this.sshServer.setPasswordAuthenticator((username, password, session) -> false);
        this.sshServer.setPublickeyAuthenticator((username, key, session) -> authenticate(username, key, session.getIoSession().getId()));
        this.sshServer.setShellFactory(channel -> new SshGameCommand(identities, terminalSession, executorService));
    }

    public void start() throws IOException {
        sshServer.start();
    }

    @Override
    public void close() throws IOException {
        sshServer.stop();
        executorService.shutdownNow();
    }

    private boolean authenticate(String username, PublicKey key, long sessionId) {
        identities.put(sessionId, new LoginIdentity(username, KeyUtils.getFingerPrint(key)));
        return true;
    }

    private static void ensureParentDirectory(Path hostKeyPath) {
        Path parent = hostKeyPath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create host key directory: " + parent, exception);
        }
    }
}

