package com.github.mattoyudzuru.terminalbang.game;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StandardContent {
    private static final Map<CardKind, CardDefinition> DEFINITIONS = definitions();

    private StandardContent() {
    }

    public static CardDefinition card(CardKind kind) {
        return DEFINITIONS.get(kind);
    }

    public static List<CardInstance> deck() {
        List<CardInstance> cards = new ArrayList<>(80);

        add(cards, CardKind.BARREL, CardSuit.SPADES, CardRank.QUEEN);
        add(cards, CardKind.BARREL, CardSuit.SPADES, CardRank.KING);
        add(cards, CardKind.DYNAMITE, CardSuit.HEARTS, CardRank.TWO);
        add(cards, CardKind.JAIL, CardSuit.SPADES, CardRank.JACK);
        add(cards, CardKind.JAIL, CardSuit.HEARTS, CardRank.FOUR);
        add(cards, CardKind.JAIL, CardSuit.SPADES, CardRank.TEN);
        add(cards, CardKind.MUSTANG, CardSuit.HEARTS, CardRank.EIGHT);
        add(cards, CardKind.MUSTANG, CardSuit.HEARTS, CardRank.NINE);
        add(cards, CardKind.REMINGTON, CardSuit.CLUBS, CardRank.KING);
        add(cards, CardKind.REV_CARABINE, CardSuit.CLUBS, CardRank.ACE);
        add(cards, CardKind.SCHOFIELD, CardSuit.CLUBS, CardRank.JACK);
        add(cards, CardKind.SCHOFIELD, CardSuit.CLUBS, CardRank.QUEEN);
        add(cards, CardKind.SCHOFIELD, CardSuit.SPADES, CardRank.KING);
        add(cards, CardKind.SCOPE, CardSuit.SPADES, CardRank.ACE);
        add(cards, CardKind.VOLCANIC, CardSuit.SPADES, CardRank.TEN);
        add(cards, CardKind.VOLCANIC, CardSuit.CLUBS, CardRank.TEN);
        add(cards, CardKind.WINCHESTER, CardSuit.SPADES, CardRank.EIGHT);

        add(cards, CardKind.BANG, CardSuit.SPADES, CardRank.ACE);
        addRange(cards, CardKind.BANG, CardSuit.DIAMONDS, CardRank.TWO, CardRank.ACE);
        addRange(cards, CardKind.BANG, CardSuit.CLUBS, CardRank.TWO, CardRank.NINE);
        addRange(cards, CardKind.BANG, CardSuit.HEARTS, CardRank.QUEEN, CardRank.ACE);
        addRange(cards, CardKind.BEER, CardSuit.HEARTS, CardRank.SIX, CardRank.JACK);
        add(cards, CardKind.CAT_BALOU, CardSuit.HEARTS, CardRank.KING);
        addRange(cards, CardKind.CAT_BALOU, CardSuit.DIAMONDS, CardRank.NINE, CardRank.JACK);
        add(cards, CardKind.DUEL, CardSuit.DIAMONDS, CardRank.QUEEN);
        add(cards, CardKind.DUEL, CardSuit.SPADES, CardRank.JACK);
        add(cards, CardKind.DUEL, CardSuit.CLUBS, CardRank.EIGHT);
        add(cards, CardKind.GATLING, CardSuit.HEARTS, CardRank.TEN);
        add(cards, CardKind.GENERAL_STORE, CardSuit.CLUBS, CardRank.NINE);
        add(cards, CardKind.GENERAL_STORE, CardSuit.SPADES, CardRank.QUEEN);
        add(cards, CardKind.INDIANS, CardSuit.DIAMONDS, CardRank.KING);
        add(cards, CardKind.INDIANS, CardSuit.DIAMONDS, CardRank.ACE);
        addRange(cards, CardKind.MISSED, CardSuit.CLUBS, CardRank.TEN, CardRank.ACE);
        addRange(cards, CardKind.MISSED, CardSuit.SPADES, CardRank.TWO, CardRank.EIGHT);
        addRange(cards, CardKind.PANIC, CardSuit.HEARTS, CardRank.JACK, CardRank.QUEEN);
        add(cards, CardKind.PANIC, CardSuit.HEARTS, CardRank.ACE);
        add(cards, CardKind.PANIC, CardSuit.DIAMONDS, CardRank.EIGHT);
        add(cards, CardKind.SALOON, CardSuit.HEARTS, CardRank.FIVE);
        add(cards, CardKind.STAGECOACH, CardSuit.SPADES, CardRank.NINE);
        add(cards, CardKind.STAGECOACH, CardSuit.SPADES, CardRank.NINE);
        add(cards, CardKind.WELLS_FARGO, CardSuit.HEARTS, CardRank.THREE);

        return List.copyOf(cards);
    }

    public static List<CharacterDefinition> characters() {
        return List.of(
                new CharacterDefinition("bart_cassidy", "Bart Cassidy", 4, "Draws one card each time he loses a life point."),
                new CharacterDefinition("black_jack", "Black Jack", 4, "Reveals his second draw; if it is red, he draws one extra card."),
                new CharacterDefinition("calamity_janet", "Calamity Janet", 4, "Can use Bang as Missed and Missed as Bang."),
                new CharacterDefinition("el_gringo", "El Gringo", 3, "When another player's card hurts him, he takes random cards from that player's hand."),
                new CharacterDefinition("jesse_jones", "Jesse Jones", 4, "At draw phase, may take the first card from another player's hand."),
                new CharacterDefinition("jourdonnais", "Jourdonnais", 4, "Always has an extra Barrel check."),
                new CharacterDefinition("kit_carlson", "Kit Carlson", 4, "Looks at three draw cards and keeps two."),
                new CharacterDefinition("lucky_duke", "Lucky Duke", 4, "Draw! checks flip two cards and use the better result."),
                new CharacterDefinition("paul_regret", "Paul Regret", 3, "Other players see him one distance farther away."),
                new CharacterDefinition("pedro_ramirez", "Pedro Ramirez", 4, "May draw his first card from the discard pile."),
                new CharacterDefinition("rose_doolan", "Rose Doolan", 4, "Sees every other player one distance closer."),
                new CharacterDefinition("sid_ketchum", "Sid Ketchum", 4, "Can discard two cards to recover one life point."),
                new CharacterDefinition("slab_the_killer", "Slab the Killer", 4, "His Bang cards require two Missed effects to cancel."),
                new CharacterDefinition("suzy_lafayette", "Suzy Lafayette", 4, "Whenever her hand becomes empty, she draws one card."),
                new CharacterDefinition("vulture_sam", "Vulture Sam", 4, "Takes the hand and in-play cards of eliminated players."),
                new CharacterDefinition("willy_the_kid", "Willy the Kid", 4, "Can play any number of Bang cards during his turn.")
        );
    }

    private static Map<CardKind, CardDefinition> definitions() {
        Map<CardKind, CardDefinition> definitions = new EnumMap<>(CardKind.class);
        definitions.put(CardKind.BANG, new CardDefinition(CardKind.BANG, "Bang!", "BROWN", "Attack a player within weapon range. The target may answer with Missed.", true, false));
        definitions.put(CardKind.MISSED, new CardDefinition(CardKind.MISSED, "Missed!", "REACTION", "Cancel one Bang-style attack aimed at you.", false, true));
        definitions.put(CardKind.BEER, new CardDefinition(CardKind.BEER, "Beer", "BROWN", "Recover one life point. Cannot be played at full health and has no effect with only two players alive.", false, false));
        definitions.put(CardKind.CAT_BALOU, new CardDefinition(CardKind.CAT_BALOU, "Cat Balou", "BROWN", "Force any one player to discard one random hand or in-play card.", true, false));
        definitions.put(CardKind.DUEL, new CardDefinition(CardKind.DUEL, "Duel", "BROWN", "Challenge any player. Players alternate Bang responses until one fails and loses one life.", true, false));
        definitions.put(CardKind.GATLING, new CardDefinition(CardKind.GATLING, "Gatling", "BROWN", "All other players must answer with Missed or lose one life.", false, false));
        definitions.put(CardKind.GENERAL_STORE, new CardDefinition(CardKind.GENERAL_STORE, "General Store", "BROWN", "Reveal one card per alive player; each player chooses one card in turn order.", false, false));
        definitions.put(CardKind.INDIANS, new CardDefinition(CardKind.INDIANS, "Indians!", "BROWN", "All other players must discard Bang or lose one life. Missed and Barrel do not help.", false, false));
        definitions.put(CardKind.PANIC, new CardDefinition(CardKind.PANIC, "Panic!", "BROWN", "Take one random hand or in-play card from a player at distance 1.", true, false));
        definitions.put(CardKind.SALOON, new CardDefinition(CardKind.SALOON, "Saloon", "BROWN", "Every alive player recovers one life point.", false, false));
        definitions.put(CardKind.STAGECOACH, new CardDefinition(CardKind.STAGECOACH, "Stagecoach", "BROWN", "Draw two cards.", false, false));
        definitions.put(CardKind.WELLS_FARGO, new CardDefinition(CardKind.WELLS_FARGO, "Wells Fargo", "BROWN", "Draw three cards.", false, false));
        definitions.put(CardKind.BARREL, new CardDefinition(CardKind.BARREL, "Barrel", "BLUE", "In play: when answering a Bang-style attack, draw! hearts count as one Missed.", false, false));
        definitions.put(CardKind.DYNAMITE, new CardDefinition(CardKind.DYNAMITE, "Dynamite", "BLUE", "In play: at turn start, draw! spades 2-9 explodes for 3 damage; otherwise pass it left.", false, false));
        definitions.put(CardKind.JAIL, new CardDefinition(CardKind.JAIL, "Jail", "BLUE", "Play in front of another non-Sheriff. At turn start, hearts escape; otherwise skip turn.", true, false));
        definitions.put(CardKind.MUSTANG, new CardDefinition(CardKind.MUSTANG, "Mustang", "BLUE", "In play: other players see you one distance farther away.", false, false));
        definitions.put(CardKind.SCOPE, new CardDefinition(CardKind.SCOPE, "Scope", "BLUE", "In play: you see every other player one distance closer.", false, false));
        definitions.put(CardKind.REMINGTON, new CardDefinition(CardKind.REMINGTON, "Remington", "WEAPON", "Weapon range 3.", false, false));
        definitions.put(CardKind.REV_CARABINE, new CardDefinition(CardKind.REV_CARABINE, "Rev. Carabine", "WEAPON", "Weapon range 4.", false, false));
        definitions.put(CardKind.SCHOFIELD, new CardDefinition(CardKind.SCHOFIELD, "Schofield", "WEAPON", "Weapon range 2.", false, false));
        definitions.put(CardKind.VOLCANIC, new CardDefinition(CardKind.VOLCANIC, "Volcanic", "WEAPON", "Weapon range 1, but you may play any number of Bang cards.", false, false));
        definitions.put(CardKind.WINCHESTER, new CardDefinition(CardKind.WINCHESTER, "Winchester", "WEAPON", "Weapon range 5.", false, false));
        return Map.copyOf(definitions);
    }

    private static void add(List<CardInstance> cards, CardKind kind, CardSuit suit, CardRank rank) {
        cards.add(CardInstance.create(card(kind), suit, rank));
    }

    private static void addRange(List<CardInstance> cards, CardKind kind, CardSuit suit, CardRank from, CardRank to) {
        CardRank[] ranks = CardRank.values();
        for (int i = from.ordinal(); i <= to.ordinal(); i++) {
            add(cards, kind, suit, ranks[i]);
        }
    }
}
