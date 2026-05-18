package com.github.mattoyudzuru.terminalbang.game;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public interface RandomSource {
    int nextInt(int bound);

    <T> void shuffle(List<T> values);

    static RandomSource seeded(long seed) {
        return new JavaRandomSource(new Random(seed));
    }

    static RandomSource system() {
        return new JavaRandomSource(new Random());
    }

    final class JavaRandomSource implements RandomSource {
        private final Random random;

        private JavaRandomSource(Random random) {
            this.random = random;
        }

        @Override
        public int nextInt(int bound) {
            return random.nextInt(bound);
        }

        @Override
        public <T> void shuffle(List<T> values) {
            Collections.shuffle(values, random);
        }
    }
}

