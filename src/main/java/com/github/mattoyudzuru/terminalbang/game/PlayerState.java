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
    private final List<CardInstance> inPlay = new ArrayList<>();

    private int health;
    private boolean connected = true;
    private boolean eliminated;
    private Optional<Instant> disconnectedAt = Optional.empty();
    private int damageDealt;
    private int damageTaken;
    private int cardsPlayed;
    private int bangCardsPlayedThisTurn;

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

    public List<CardInstance> inPlay() {
        return List.copyOf(inPlay);
    }

    public int handSize() {
        return hand.size();
    }

    public int inPlaySize() {
        return inPlay.size();
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

    boolean hasInPlay(CardKind kind) {
        return inPlay.stream().anyMatch(card -> card.kind() == kind);
    }

    void addInPlay(CardInstance card) {
        inPlay.add(card);
    }

    Optional<CardInstance> removeInPlay(CardKind kind) {
        for (int i = 0; i < inPlay.size(); i++) {
            if (inPlay.get(i).kind() == kind) {
                return Optional.of(inPlay.remove(i));
            }
        }
        return Optional.empty();
    }

    Optional<CardInstance> removeWeaponInPlay() {
        for (int i = 0; i < inPlay.size(); i++) {
            if (isWeapon(inPlay.get(i).kind())) {
                return Optional.of(inPlay.remove(i));
            }
        }
        return Optional.empty();
    }

    Optional<CardInstance> removeRandomVisibleCard(RandomSource random) {
        int total = hand.size() + inPlay.size();
        if (total == 0) {
            return Optional.empty();
        }
        int index = random.nextInt(total);
        if (index < hand.size()) {
            return Optional.of(hand.remove(index));
        }
        return Optional.of(inPlay.remove(index - hand.size()));
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

    List<CardInstance> removeAllCards() {
        List<CardInstance> cards = new ArrayList<>(hand.size() + inPlay.size());
        cards.addAll(hand);
        cards.addAll(inPlay);
        hand.clear();
        inPlay.clear();
        return cards;
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

    void recordBangCardPlayed() {
        bangCardsPlayedThisTurn++;
    }

    void resetTurnFlags() {
        bangCardsPlayedThisTurn = 0;
    }

    boolean canPlayBangCard() {
        return bangCardsPlayedThisTurn == 0
                || hasInPlay(CardKind.VOLCANIC)
                || character.id().equals("willy_the_kid");
    }

    int weaponRange() {
        return inPlay.stream()
                .filter(card -> isWeapon(card.kind()))
                .mapToInt(card -> weaponRange(card.kind()))
                .findFirst()
                .orElse(1);
    }

    int outgoingDistanceModifier() {
        int modifier = hasInPlay(CardKind.SCOPE) ? -1 : 0;
        if (character.id().equals("rose_doolan")) {
            modifier--;
        }
        return modifier;
    }

    int incomingDistanceModifier() {
        int modifier = hasInPlay(CardKind.MUSTANG) ? 1 : 0;
        if (character.id().equals("paul_regret")) {
            modifier++;
        }
        return modifier;
    }

    static boolean isWeapon(CardKind kind) {
        return switch (kind) {
            case REMINGTON, REV_CARABINE, SCHOFIELD, VOLCANIC, WINCHESTER -> true;
            default -> false;
        };
    }

    private static int weaponRange(CardKind kind) {
        return switch (kind) {
            case SCHOFIELD -> 2;
            case REMINGTON -> 3;
            case REV_CARABINE -> 4;
            case WINCHESTER -> 5;
            case VOLCANIC -> 1;
            default -> 1;
        };
    }
}
