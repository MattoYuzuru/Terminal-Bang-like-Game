package com.github.mattoyudzuru.terminalbang.tui;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class SessionIo {
    private final InputStream inputStream;
    private final OutputStream outputStream;

    SessionIo(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    void write(String text) throws IOException {
        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    TerminalKey readKey() throws IOException {
        int first = inputStream.read();
        if (first < 0) {
            throw new EOFException("SSH session closed");
        }
        if (first == 3) {
            return TerminalKey.ctrlC();
        }
        if (first == '\r' || first == '\n') {
            return TerminalKey.enter();
        }
        if (first == 8 || first == 127) {
            return TerminalKey.backspace();
        }
        if (first == '\t') {
            return TerminalKey.tab();
        }
        if (first == 27) {
            return readEscape();
        }
        char character = (char) first;
        if (character >= '1' && character <= '9') {
            return TerminalKey.digit(character - '1');
        }
        return TerminalKey.character(Character.toLowerCase(character));
    }

    String readLine(String prompt) throws IOException {
        write(prompt);
        StringBuilder builder = new StringBuilder();
        while (true) {
            TerminalKey key = readKey();
            if (key.type() == TerminalKeyType.ENTER) {
                write("\r\n");
                return builder.toString();
            }
            if (key.type() == TerminalKeyType.BACKSPACE) {
                if (!builder.isEmpty()) {
                    builder.deleteCharAt(builder.length() - 1);
                    write("\b \b");
                }
                continue;
            }
            if (key.type() == TerminalKeyType.CHARACTER || key.type() == TerminalKeyType.DIGIT) {
                char character = key.type() == TerminalKeyType.DIGIT ? (char) ('1' + key.digitIndex()) : key.character();
                builder.append(character);
                write(String.valueOf(character));
            }
        }
    }

    private TerminalKey readEscape() throws IOException {
        int second = inputStream.read();
        if (second != '[') {
            return TerminalKey.escape();
        }
        int third = inputStream.read();
        return switch (third) {
            case 'D' -> TerminalKey.left();
            case 'C' -> TerminalKey.right();
            default -> TerminalKey.escape();
        };
    }
}

