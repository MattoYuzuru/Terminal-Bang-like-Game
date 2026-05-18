package com.github.mattoyudzuru.terminalbang.tui;

public record TerminalKey(TerminalKeyType type, char character, int digitIndex) {
    public static TerminalKey character(char character) {
        return new TerminalKey(TerminalKeyType.CHARACTER, character, -1);
    }

    public static TerminalKey digit(int digitIndex) {
        return new TerminalKey(TerminalKeyType.DIGIT, (char) ('1' + digitIndex), digitIndex);
    }

    public static TerminalKey enter() {
        return new TerminalKey(TerminalKeyType.ENTER, '\n', -1);
    }

    public static TerminalKey backspace() {
        return new TerminalKey(TerminalKeyType.BACKSPACE, '\b', -1);
    }

    public static TerminalKey left() {
        return new TerminalKey(TerminalKeyType.LEFT, '\0', -1);
    }

    public static TerminalKey right() {
        return new TerminalKey(TerminalKeyType.RIGHT, '\0', -1);
    }

    public static TerminalKey tab() {
        return new TerminalKey(TerminalKeyType.TAB, '\t', -1);
    }

    public static TerminalKey escape() {
        return new TerminalKey(TerminalKeyType.ESCAPE, (char) 27, -1);
    }

    public static TerminalKey ctrlC() {
        return new TerminalKey(TerminalKeyType.CTRL_C, (char) 3, -1);
    }

    public boolean isCharacter(char expected) {
        return type == TerminalKeyType.CHARACTER && character == expected;
    }
}

