package com.github.mattoyudzuru.terminalbang.tui;

import com.github.mattoyudzuru.terminalbang.game.CardInstance;
import com.github.mattoyudzuru.terminalbang.game.CardKind;
import com.github.mattoyudzuru.terminalbang.game.CharacterDefinition;
import com.github.mattoyudzuru.terminalbang.game.GamePhase;
import com.github.mattoyudzuru.terminalbang.game.Role;
import com.github.mattoyudzuru.terminalbang.game.WinningSide;
import com.github.mattoyudzuru.terminalbang.user.Account;

final class I18n {
    private I18n() {
    }

    static String lang(Account account) {
        return account == null ? "en" : lang(account.language());
    }

    static String lang(String language) {
        return "ru".equalsIgnoreCase(language) ? "ru" : "en";
    }

    static String t(String language, String en, String ru) {
        return "ru".equals(lang(language)) ? ru : en;
    }

    static String languageName(String language) {
        return t(language, "English", "Русский");
    }

    static String role(String language, Role role) {
        if (!"ru".equals(lang(language))) {
            return role.name();
        }
        return switch (role) {
            case SHERIFF -> "ШЕРИФ";
            case DEPUTY -> "ПОМОЩНИК";
            case OUTLAW -> "БАНДИТ";
            case RENEGADE -> "РЕНЕГАТ";
        };
    }

    static String phase(String language, GamePhase phase) {
        if (!"ru".equals(lang(language))) {
            return phase.name();
        }
        return switch (phase) {
            case PLAY -> "ХОД";
            case DISCARD -> "СБРОС";
            case FINISHED -> "КОНЕЦ";
        };
    }

    static String winner(String language, WinningSide winner) {
        if (!"ru".equals(lang(language))) {
            return winner.name();
        }
        return switch (winner) {
            case LAW -> "ЗАКОН";
            case OUTLAWS -> "БАНДИТЫ";
            case RENEGADE -> "РЕНЕГАТ";
            case ABANDONED -> "ПОКИНУТО";
        };
    }

    static String cardName(String language, CardKind kind) {
        if (!"ru".equals(lang(language))) {
            return switch (kind) {
                case BANG -> "Bang!";
                case MISSED -> "Missed!";
                case CAT_BALOU -> "Cat Balou";
                case GENERAL_STORE -> "General Store";
                case WELLS_FARGO -> "Wells Fargo";
                case REV_CARABINE -> "Rev. Carabine";
                default -> title(kind.name());
            };
        }
        return switch (kind) {
            case BANG -> "Бэнг!";
            case MISSED -> "Мимо!";
            case BEER -> "Пиво";
            case CAT_BALOU -> "Кэт Баллу";
            case DUEL -> "Дуэль";
            case GATLING -> "Гатлинг";
            case GENERAL_STORE -> "Магазин";
            case INDIANS -> "Индейцы!";
            case PANIC -> "Паника!";
            case SALOON -> "Салун";
            case STAGECOACH -> "Дилижанс";
            case WELLS_FARGO -> "Уэллс Фарго";
            case BARREL -> "Бочка";
            case DYNAMITE -> "Динамит";
            case JAIL -> "Тюрьма";
            case MUSTANG -> "Мустанг";
            case SCOPE -> "Прицел";
            case REMINGTON -> "Ремингтон";
            case REV_CARABINE -> "Карабин";
            case SCHOFIELD -> "Скофилд";
            case VOLCANIC -> "Волканик";
            case WINCHESTER -> "Винчестер";
        };
    }

    static String cardName(String language, CardInstance card) {
        return cardName(language, card.kind());
    }

    static String cardDescription(String language, CardInstance card) {
        if (!"ru".equals(lang(language))) {
            return card.definition().description();
        }
        return switch (card.kind()) {
            case BANG -> "Атакует игрока в пределах дальности оружия. Цель может ответить картой Мимо!.";
            case MISSED -> "Отменяет одну атаку Бэнг, направленную в тебя.";
            case BEER -> "Восстанавливает 1 жизнь. Нельзя играть на полном здоровье; при двух живых игроках не помогает.";
            case CAT_BALOU -> "Заставляет любого игрока сбросить случайную карту из руки или со стола.";
            case DUEL -> "Вызывает любого игрока на дуэль. Игроки по очереди сбрасывают Бэнг; первый отказавшийся теряет 1 жизнь.";
            case GATLING -> "Все остальные игроки должны ответить Мимо! или потерять 1 жизнь.";
            case GENERAL_STORE -> "Открывает по карте на каждого живого игрока; каждый выбирает одну карту по очереди.";
            case INDIANS -> "Все остальные игроки должны сбросить Бэнг или потерять 1 жизнь. Мимо! и Бочка не помогают.";
            case PANIC -> "Забирает случайную карту из руки или со стола у игрока на дистанции 1.";
            case SALOON -> "Все живые игроки восстанавливают 1 жизнь.";
            case STAGECOACH -> "Возьми 2 карты.";
            case WELLS_FARGO -> "Возьми 3 карты.";
            case BARREL -> "На столе: при атаке Бэнг сделай draw!; черва считается одной Мимо!.";
            case DYNAMITE -> "На столе: в начале хода draw!; пики 2-9 взрываются на 3 урона, иначе Динамит передается дальше.";
            case JAIL -> "Кладется перед другим игроком, кроме Шерифа. В начале хода черва выпускает, иначе ход пропускается.";
            case MUSTANG -> "На столе: другие игроки видят тебя на 1 дистанцию дальше.";
            case SCOPE -> "На столе: ты видишь остальных игроков на 1 дистанцию ближе.";
            case REMINGTON -> "Оружие с дальностью 3.";
            case REV_CARABINE -> "Оружие с дальностью 4.";
            case SCHOFIELD -> "Оружие с дальностью 2.";
            case VOLCANIC -> "Оружие с дальностью 1, зато можно играть любое число Бэнг за ход.";
            case WINCHESTER -> "Оружие с дальностью 5.";
        };
    }

    static String characterName(String language, CharacterDefinition character) {
        if (!"ru".equals(lang(language))) {
            return character.name();
        }
        return switch (character.id()) {
            case "bart_cassidy" -> "Барт Кэссиди";
            case "black_jack" -> "Блэк Джек";
            case "calamity_janet" -> "Каламити Джанет";
            case "el_gringo" -> "Эль Гринго";
            case "jesse_jones" -> "Джесси Джонс";
            case "jourdonnais" -> "Журдоннэ";
            case "kit_carlson" -> "Кит Карлсон";
            case "lucky_duke" -> "Лаки Дюк";
            case "paul_regret" -> "Пол Регрет";
            case "pedro_ramirez" -> "Педро Рамирес";
            case "rose_doolan" -> "Роуз Дулан";
            case "sid_ketchum" -> "Сид Кетчум";
            case "slab_the_killer" -> "Слэб Убийца";
            case "suzy_lafayette" -> "Сьюзи Лафайет";
            case "vulture_sam" -> "Стервятник Сэм";
            case "willy_the_kid" -> "Вилли Кид";
            default -> character.name();
        };
    }

    static String characterDescription(String language, CharacterDefinition character) {
        if (!"ru".equals(lang(language))) {
            return character.description();
        }
        return switch (character.id()) {
            case "bart_cassidy" -> "Берет 1 карту каждый раз, когда теряет жизнь.";
            case "black_jack" -> "Показывает вторую карту добора; если она красная, берет еще 1 карту.";
            case "calamity_janet" -> "Может использовать Бэнг как Мимо! и Мимо! как Бэнг.";
            case "el_gringo" -> "Когда карта другого игрока наносит ему урон, берет случайные карты из руки этого игрока.";
            case "jesse_jones" -> "В фазе добора может взять первую карту из руки другого игрока.";
            case "jourdonnais" -> "Всегда имеет дополнительную проверку Бочки.";
            case "kit_carlson" -> "Смотрит 3 верхние карты колоды и оставляет себе 2.";
            case "lucky_duke" -> "При draw! открывает 2 карты и использует лучший результат.";
            case "paul_regret" -> "Другие игроки видят его на 1 дистанцию дальше.";
            case "pedro_ramirez" -> "Может взять первую карту добора из сброса.";
            case "rose_doolan" -> "Видит остальных игроков на 1 дистанцию ближе.";
            case "sid_ketchum" -> "Может сбросить 2 карты, чтобы восстановить 1 жизнь.";
            case "slab_the_killer" -> "Его Бэнг требует 2 эффекта Мимо! для отмены.";
            case "suzy_lafayette" -> "Когда рука становится пустой, берет 1 карту.";
            case "vulture_sam" -> "Забирает карты руки и стола выбывших игроков.";
            case "willy_the_kid" -> "Может играть любое число Бэнг за ход.";
            default -> character.description();
        };
    }

    static String cardHelp(String language, CardInstance card) {
        return cardName(language, card) + ": " + cardDescription(language, card);
    }

    private static String title(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
