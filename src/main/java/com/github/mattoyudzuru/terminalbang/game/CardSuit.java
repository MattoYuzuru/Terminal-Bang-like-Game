package com.github.mattoyudzuru.terminalbang.game;

public enum CardSuit {
    SPADES("S", false),
    HEARTS("H", true),
    DIAMONDS("D", true),
    CLUBS("C", false);

    private final String symbol;
    private final boolean red;

    CardSuit(String symbol, boolean red) {
        this.symbol = symbol;
        this.red = red;
    }

    public String symbol() {
        return symbol;
    }

    public boolean red() {
        return red;
    }
}
