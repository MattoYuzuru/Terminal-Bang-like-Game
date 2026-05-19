# Terminal Western Card Game

Неофициальная multiplayer TUI-игра в стиле классической western social-deduction card game. Игрок подключается по SSH, попадает не в shell, а в терминальный интерфейс с меню, комнатами, матчем, reconnect-механикой и профилем.

> Важно: проект не должен публично использовать оригинальные тексты, арты, названия карт/персонажей, логотипы и брендинг BANG!/dV Giochi без разрешения. Для публичного релиза нужны собственные названия, тексты, оформление и домен без чужого бренда.

## Проверенные решения

- База правил ориентируется на 4-7 игроков: Sheriff, Deputies, Outlaws, Renegade.
- Sheriff раскрыт всем сразу; остальные роли скрыты до смерти или конца игры.
- Ход игрока: draw phase, play phase, discard phase.
- Лимит руки в конце хода равен текущему количеству жизней.
- Расстояние между игроками считается по кругу и влияет на часть карт.
- Колода базовой игры фиксированная: 80 карт с конкретными количествами, мастями и рангами.
- Blue/weapon cards лежат перед игроком; weapon range не меняет distance, а только максимальную дальность Bang.
- Приложение пишется на Java, но без HTTP-first архитектуры.
- Spring Boot не нужен для MVP; его можно добавить позже только ради DI/config/metrics, с `web-application-type=none`.
- PostgreSQL используется для постоянных данных. Активная игра в MVP хранится в памяти процесса.
- Для запуска с PostgreSQL предпочтителен `docker compose`, а не один `Dockerfile`.

## Цель MVP

Сделать играбельный SSH-сервер:

```bash
ssh -p 2222 game.example.com
```

Минимальный состав:

- авторизация по SSH public key fingerprint;
- создание профиля и nickname;
- главное меню;
- создание приватной комнаты с 5-буквенным кодом;
- переключение комнаты в public;
- каталог публичных комнат;
- kick игроков host'ом;
- старт игры при достижении минимума игроков;
- базовый матч на 4-7 игроков;
- роли, персонажи, жизни, рука, колода, сброс;
- выбор карты, выбор цели, реакции;
- reconnect по тому же SSH-ключу;
- сохранение результата и статистики матча.

## Быстрый запуск

Локально нужен PostgreSQL:

```bash
createdb terminal_bang
export TB_DB_URL=jdbc:postgresql://localhost:5432/terminal_bang
export TB_DB_USER=terminal_bang
export TB_DB_PASSWORD=terminal_bang
gradle run
```

Через Docker Compose:

```bash
docker compose up --build
ssh -p 2222 game.example.com
```

Для локальной проверки:

```bash
ssh -p 2222 localhost
```

Переменные окружения:

```text
TB_SSH_PORT       default 2222
TB_HOST_KEY_PATH  default data/hostkey.ser
TB_DB_URL         default jdbc:postgresql://localhost:5432/terminal_bang
TB_DB_USER        default terminal_bang
TB_DB_PASSWORD    default terminal_bang
TB_DB_POOL_SIZE   default 8
```

## Реализовано сейчас

- Gradle Java application.
- PostgreSQL schema + Flyway migrations.
- SSH server на Apache MINA SSHD.
- Public key auth по SSH fingerprint.
- Account/profile по fingerprint.
- Главное меню, профиль, settings nickname, leaderboard.
- Create/join private room по 5-буквенному коду.
- Public rooms catalog.
- Host controls: public/private toggle, start, kick, close room.
- Game engine: 4-7 игроков, роли, стартовый Sheriff, ходы, рука, колода, сброс.
- Базовая колода на 80 карт: Bang, Missed, Beer, Cat Balou, Duel, Gatling, General Store, Indians, Panic, Saloon, Stagecoach, Wells Fargo, Barrel, Dynamite, Jail, Mustang, Scope и все weapons.
- 16 базовых персонажей и их способности: Bart Cassidy, Black Jack, Calamity Janet, El Gringo, Jesse Jones, Jourdonnais, Kit Carlson, Lucky Duke, Paul Regret, Pedro Ramirez, Rose Doolan, Sid Ketchum, Slab the Killer, Suzy Lafayette, Vulture Sam, Willy the Kid.
- Target selection, pending reactions, mass reactions, discard phase.
- Edge cases: Beer lethal-save, Beer off with two alive, one Bang per turn, Volcanic/Willy exceptions, Slab two-Missed rule, Jail not on Sheriff, Dynamite before Jail, Barrel/Jourdonnais draw!, Mustang/Scope distance modifiers, Outlaw reward, Sheriff penalty for killing Deputy, Vulture Sam card pickup.
- Disconnect/reconnect state, timeout skip, abandoned match.
- Match stats persistence.
- Unit tests для engine, rooms и renderer.

Ограничения текущего MVP:

- TUI пока простой ANSI-интерфейс, не финальный красивый веер карт.
- Resize определяется по начальному `COLUMNS/LINES`; live resize через WINCH ещё не реализован.
- General Store, Jesse Jones и Kit Carlson в MVP выбирают карты автоматически, потому что интерактивный выбор для этих эффектов ещё не вынесен в отдельный pending-screen.
- Нет AI/bot игроков; для матча нужны реальные SSH-сессии.
- Нет рейтинга, spectator mode и чата.

## Правила MVP

Внутренняя модель сверена с официальными материалами:

- правила Fourth edition: `https://bang.dvgiochi.com/content/1/docs/01_bang_rules_EN.pdf`;
- официальный список карт: `https://bang.dvgames.com/cardslist.php?id=1&lang=en`.

В публичном UI используются краткие пересказы эффектов. Оригинальные арты, логотипы и длинные тексты правил не копируются.

Колода:

```text
Brown/reaction: Bang x25, Missed x12, Beer x6, Cat Balou x4, Duel x3,
Gatling x1, General Store x2, Indians x2, Panic x4, Saloon x1,
Stagecoach x2, Wells Fargo x1.

Blue/weapon: Barrel x2, Dynamite x1, Jail x3, Mustang x2, Scope x1,
Volcanic x2, Schofield x3, Remington x1, Rev. Carabine x1, Winchester x1.
```

Порядок turn start:

```text
1. Dynamite draw! check, если карта лежит перед игроком.
2. Jail draw! check, если игрок в Jail.
3. Draw phase с учетом способности персонажа.
4. Play phase.
5. Discard до текущего HP.
```

Игрок видит свою роль, персонажа, HP, карты в руке, карты in-play и строку `Ability` с описанием способности персонажа.

## Управление

```text
← / →       выбрать карту или цель
1-9         выбрать карту из руки
Enter       подтвердить действие
Backspace   назад / отмена
? или /      описание выбранной карты
Tab         переключение UI-зоны, если понадобится
E           закончить ход
Q           меню выхода
```

Сценарий карты с целью:

```text
выбор карты -> Enter -> выбор игрока -> Enter
Backspace отменяет выбор цели и возвращает к руке
```

Если карта требует сброса:

```text
выбор карты -> Enter сбросить
Backspace отменяет, если правило позволяет
```

## TUI

Основной экран делится на:

- центральный стол с игроками по овалу;
- колоду и стопку сброса в центре;
- нижнюю руку игрока;
- правую зону состояния: роль, персонаж, оружие/эффекты, жизни;
- нижнюю строку подсказок по клавишам.

Требования:

- рекомендуемый размер: `120x40`;
- минимальный размер: `90x30`;
- ниже минимума показывается экран resize-warning;
- layout пересчитывается при resize;
- цвета должны иметь fallback для простых терминалов;
- анимации ограничены короткими переходами: поднятие карты, смена фокуса, pop-up.

Веер карт в MVP делается как overlapping cards: карты частично перекрываются, выбранная карта поднимается на 1-2 строки. Настоящий геометрический веер можно оставить на polish-этап.

## Reconnect

Модель разделяет:

- `Account` - постоянный пользователь по SSH fingerprint;
- `Connection` - текущее SSH-соединение;
- `Seat` - место игрока в конкретной игре.

При disconnect seat сохраняется.

Если игрок отключился во время своего хода:

```text
60 секунд ожидания
если reconnect - ход продолжается
если timeout - auto-discard до лимита руки и end turn
```

Если игрок отключился не в свой ход, таймер запускается, когда ход доходит до него. Если все игроки вышли, матч завершается как abandoned.

## Данные

PostgreSQL:

- accounts;
- profiles;
- rooms history;
- match results;
- player match stats;
- leaderboard;
- card definitions;
- character definitions;
- migrations.

Память процесса:

- active rooms;
- active games;
- turn state;
- pending reactions;
- connected sessions;
- timers.

Позже можно добавить snapshots/event log для восстановления активных матчей после рестарта сервера.

## Стек

- Java 21+
- Gradle
- Apache MINA SSHD - embedded SSH server
- JLine - terminal input/output, size, capabilities, virtual terminal support
- собственный retained/diff renderer для игрового TUI
- PostgreSQL
- HikariCP
- Flyway
- Jdbi или jOOQ для persistence layer
- JUnit 5
- AssertJ
- Testcontainers PostgreSQL
- Dockerfile для app image
- Docker Compose для app + PostgreSQL
- GitHub Actions

Lanterna можно попробовать для прототипа, но для игрового layered UI лучше не зависеть от GUI2-компонентов и держать рендеринг под контролем.

## Архитектура

```text
src/main/java/...
  app/            bootstrap, config, lifecycle
  ssh/            ssh server, auth, session adapter
  tui/            renderer, layout, input, screens, widgets
  room/           rooms, lobby, matchmaking
  game/           game state, turn engine, rules, actions
  game/card/      card definitions and effects
  game/role/      roles and win conditions
  game/player/    seats, hands, health, visibility
  persistence/    repositories, migrations boundary
  stats/          match stats and leaderboard
```

Принципы:

- игровая логика не знает про SSH, TUI и PostgreSQL;
- UI отправляет команды, engine возвращает события;
- все случайные решения идут через injectable `RandomSource`;
- card effects реализуются как маленькие стратегии;
- реакции моделируются через `PendingAction`;
- persistence не является источником истины для активного хода.

## Тесты

Обязательно покрыть:

- распределение ролей для 4/5/6/7 игроков;
- победные условия;
- расчет расстояния;
- draw/play/discard phases;
- лимит руки по текущим жизням;
- эффекты базовых карт;
- реакции на атаки;
- создание public/private rooms;
- join по коду;
- disconnect/reconnect;
- auto-discard и skip turn;
- запись результата в PostgreSQL.

UI тестируется отдельно через layout/snapshot checks:

- `120x40` full layout;
- `90x30` compact layout;
- below minimum resize-warning.

## План

1. Bootstrap проекта: Gradle, Java 21, CI, Dockerfile, compose, PostgreSQL, migrations.
2. Domain model: roles, players, cards, deck, turn state, win conditions.
3. Game engine tests без UI и SSH.
4. SSH server: key auth, session lifecycle, reconnect identity.
5. TUI renderer: меню, комнаты, игровой экран, resize handling.
6. Room service: create/join/public/private/kick/start.
7. Базовый набор карт и reactions.
8. Persistence: profiles, stats, match results.
9. Polish: animations, themes, better card fan, compact mode.

## Источники для факт-чека

- Official BANG! rules: https://bang.dvgiochi.com/content/1/docs/01_bang_rules_EN.pdf
- dV Giochi fan/IP guidance: https://www.dvgiochi.com/giochi/bang/download/BANG%21%20fan%20contribution%20ENG.pdf
- Apache MINA SSHD: https://mina.apache.org/sshd-project/
- JLine terminal docs: https://jline.org/docs/terminal/
- Lanterna: https://github.com/mabe02/lanterna
- Spring Boot web server disabling: https://docs.spring.io/spring-boot/how-to/webserver.html
- Testcontainers PostgreSQL: https://java.testcontainers.org/modules/databases/postgres/
- PostgreSQL Docker image: https://hub.docker.com/_/postgres
