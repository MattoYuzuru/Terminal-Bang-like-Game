package com.github.mattoyudzuru.terminalbang.tui;

import com.github.mattoyudzuru.terminalbang.ssh.TerminalSize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalRendererTest {
    @Test
    void rendersResizeWarningWithActualAndMinimumSize() {
        TerminalRenderer renderer = new TerminalRenderer();

        String screen = renderer.resizeWarning(new TerminalSize(70, 20));

        assertTrue(screen.contains("Minimum size: 90x30"));
        assertTrue(screen.contains("Current size: 70x20"));
    }
}

