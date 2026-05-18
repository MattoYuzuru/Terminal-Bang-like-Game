package com.github.mattoyudzuru.terminalbang.room;

import com.github.mattoyudzuru.terminalbang.game.RandomSource;

public final class RoomCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final RandomSource random;

    public RoomCodeGenerator(RandomSource random) {
        this.random = random;
    }

    public String nextCode() {
        StringBuilder builder = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}

