package com.github.mattoyudzuru.terminalbang.persistence;

public record DatabaseSettings(String url, String user, String password, int maximumPoolSize) {
}

