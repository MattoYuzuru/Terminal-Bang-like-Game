package com.github.mattoyudzuru.terminalbang.app;

import com.github.mattoyudzuru.terminalbang.game.RandomSource;
import com.github.mattoyudzuru.terminalbang.persistence.Database;
import com.github.mattoyudzuru.terminalbang.persistence.JdbcAccountRepository;
import com.github.mattoyudzuru.terminalbang.persistence.JdbcMatchResultRepository;
import com.github.mattoyudzuru.terminalbang.room.RoomCodeGenerator;
import com.github.mattoyudzuru.terminalbang.room.RoomService;
import com.github.mattoyudzuru.terminalbang.ssh.SshGameServer;
import com.github.mattoyudzuru.terminalbang.tui.TerminalRenderer;
import com.github.mattoyudzuru.terminalbang.tui.TerminalSession;
import com.zaxxer.hikari.HikariDataSource;

import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class TerminalBangApplication {
    private TerminalBangApplication() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();
        HikariDataSource dataSource = Database.connect(config.database());
        JdbcAccountRepository accountRepository = new JdbcAccountRepository(dataSource);
        JdbcMatchResultRepository matchResultRepository = new JdbcMatchResultRepository(dataSource);
        RoomService roomService = new RoomService(
                matchResultRepository,
                new RoomCodeGenerator(RandomSource.system()),
                RandomSource.system(),
                Clock.systemUTC()
        );
        TerminalSession terminalSession = new TerminalSession(
                accountRepository,
                matchResultRepository,
                roomService,
                new TerminalRenderer()
        );
        SshGameServer sshGameServer = new SshGameServer(config.sshPort(), config.hostKeyPath(), terminalSession);
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
        watchdog.scheduleAtFixedRate(roomService::tick, 1, 1, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            watchdog.shutdownNow();
            try {
                sshGameServer.close();
            } catch (java.io.IOException exception) {
                System.err.println("Failed to stop SSH server: " + exception.getMessage());
            }
            dataSource.close();
        }, "terminal-bang-shutdown"));

        sshGameServer.start();
        System.out.println("Terminal Western Card Game SSH server listening on port " + config.sshPort());
        new CountDownLatch(1).await();
    }
}
