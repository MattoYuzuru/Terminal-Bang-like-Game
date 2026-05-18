package com.github.mattoyudzuru.terminalbang.ssh;

import com.github.mattoyudzuru.terminalbang.tui.TerminalSession;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

final class SshGameCommand implements Command {
    private final Map<Long, LoginIdentity> identities;
    private final TerminalSession terminalSession;
    private final ExecutorService executorService;

    private InputStream inputStream;
    private OutputStream outputStream;
    private OutputStream errorStream;
    private ExitCallback exitCallback;
    private Future<?> task;

    SshGameCommand(
            Map<Long, LoginIdentity> identities,
            TerminalSession terminalSession,
            ExecutorService executorService
    ) {
        this.identities = identities;
        this.terminalSession = terminalSession;
        this.executorService = executorService;
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void setErrorStream(OutputStream errorStream) {
        this.errorStream = errorStream;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }

    @Override
    public void start(ChannelSession channel, Environment environment) throws IOException {
        long sessionId = channel.getSession().getIoSession().getId();
        LoginIdentity identity = identities.get(sessionId);
        if (identity == null) {
            throw new IOException("Missing SSH identity");
        }
        TerminalSize terminalSize = TerminalSize.from(environment);
        task = executorService.submit(() -> {
            int exitCode = 0;
            try {
                terminalSession.run(identity, terminalSize, inputStream, outputStream);
            } catch (RuntimeException | IOException exception) {
                exitCode = 1;
                writeError(exception);
            } finally {
                identities.remove(sessionId);
                if (exitCallback != null) {
                    exitCallback.onExit(exitCode);
                }
            }
        });
    }

    @Override
    public void destroy(ChannelSession channel) {
        if (task != null) {
            task.cancel(true);
        }
    }

    private void writeError(Exception exception) {
        if (errorStream == null) {
            return;
        }
        try {
            errorStream.write(("Session failed: " + exception.getMessage() + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            errorStream.flush();
        } catch (IOException ignored) {
            // Session is already closing.
        }
    }
}

