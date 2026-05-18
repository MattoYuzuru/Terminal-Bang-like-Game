package com.github.mattoyudzuru.terminalbang.game;

public enum Role {
    SHERIFF,
    DEPUTY,
    OUTLAW,
    RENEGADE;

    public boolean visibleAtStart() {
        return this == SHERIFF;
    }

    public boolean winsWithLaw() {
        return this == SHERIFF || this == DEPUTY;
    }
}

