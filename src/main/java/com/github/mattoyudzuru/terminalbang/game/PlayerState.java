package com.github.mattoyudzuru.terminalbang.game;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerState {
    private final UUID accountId;
    private final String nickname;
    private final Role role;
    private final CharacterDefinition character;
    private final int maxHealth;
    private final List<CardInstance> hand = new ArrayList<>();

    private int health;
    private boolean connected = true;
    private boolean eliminated;
    private Optional<Instant> disconnectedAt = Optional.empty();
    private int damageDealt;
    private int damageTaken;
    private int cardsPlayed;

    PlayerState(UUID accountId, String nickname, Role role, CharacterDefinition character, int maxHealth) {
        this.accountId = accountId;
        this.nickname = nickname;
        this.role = role;
        this.character = character;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public UUID accountId() {
        return accountId;
    }

    public String nickname() {
        return nickname;
    }

    public Role role() {
        return role;
    }

    public CharacterDefinition character() {
        return character;
    }

    public int maxHealth() {
        return maxHealth;
    }

    public int health() {
        return health;
    }

    public boolean connected() {
        return connected;
    }

    public boolean eliminated() {
        return eliminated;
    }

    public Optional<Instant> disconnectedAt() {
        return disconnectedAt;
    }

    public List<CardInstance> hand() {
        return List.copyOf(hand);
    }

    public int handSize() {
        return hand.size();
    }

    public int damageDealt() {
        return damageDealt;
    }

    public int damageTaken() {
        return damageTaken;
    }

    public int cardsPlayed() {
        return cardsPlayed;
    }

    CardInstance cardAt(int handIndex) {
        if (handIndex < 0 || handIndex >= hand.size()) {
            throw new IllegalArgumentException("Card index out of range");
        }
        return hand.get(handIndex);
    }

    void addCard(CardInstance card) {
        hand.add(card);
    }

    CardInstance removeCard(int handIndex) {
        if (handIndex < 0 || handIndex >= hand.size()) {
            throw new IllegalArgumentException("Card index out of range");
        }
        return hand.remove(handIndex);
    }

    Optional<CardInstance> removeFirst(CardKind kind) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).kind() == kind) {
                return Optional.of(hand.remove(i));
            }
        }
        return Optional.empty();
    }

    Optional<CardInstance> removeRandomCard(RandomSource random) {
        if (hand.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(hand.remove(random.nextInt(hand.size())));
    }

    void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    void takeDamage(int amount) {
        damageTaken += amount;
        health -= amount;
    }

    void eliminate() {
        eliminated = true;
        connected = false;
        health = 0;
    }

    void disconnect(Instant at) {
        connected = false;
        disconnectedAt = Optional.of(at);
    }

    void reconnect() {
        if (!eliminated) {
            connected = true;
        }
        disconnectedAt = Optional.empty();
    }

    void recordDamageDealt(int amount) {
        damageDealt += amount;
    }

    void recordCardPlayed() {
        cardsPlayed++;
    }
}

