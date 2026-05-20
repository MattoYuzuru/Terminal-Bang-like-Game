# Terminal Western Card Game

Мультиплеерная пошаговая карточная игра в терминале, доступная через SSH. Пользователь подключается не в shell, а в TUI-интерфейс: профиль, настройки языка, комнаты, матч, reconnect и статистика.

## Важно

Это не оригинальная игра **BANG!** и не официальный продукт dV Giochi. Проект не использует оригинальные арты, логотипы и тексты правил. Игровые механики вдохновлены классической western card game; для публичного релиза нужны собственные названия, тексты и оформление.

## Демо

Демо-сервер:

```bash
ssh -p 2222 shabang.keykomi.com
```

Сертификат не нужен: это SSH, а не HTTPS. При первом подключении SSH покажет стандартный запрос доверия к host key.

Если на компьютере еще нет SSH-ключа, сначала создайте его:

```bash
ssh-keygen -t ed25519 -C "your-email@example.com"
ssh -p 2222 shabang.keykomi.com
```

Если нужен отдельный ключ для тестового игрока:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/shabang_p3 -N ""
ssh -i ~/.ssh/shabang_p3 -o IdentitiesOnly=yes -p 2222 p3@shabang.keykomi.com
```

Аккаунт привязывается к fingerprint публичного SSH-ключа. Username из SSH-команды используется как дефолтный nickname. Пароль не нужен.

## Запуск с нуля на VPS

Требования:

- Linux VPS;
- домен или поддомен с A-записью на IP сервера;
- открытый TCP-порт `2222` в firewall/security group;
- Git;
- Docker и Docker Compose plugin.

Пример для Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y git ca-certificates curl docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

После добавления пользователя в группу `docker` нужно перелогиниться в SSH-сессию. Либо запускайте команды Docker через `sudo`.

Клонирование и запуск:

```bash
git clone https://github.com/MattoYuzuru/Terminal-Bang-like-Game.git
cd Terminal-Bang-like-Game
docker compose up -d --build
```

Проверка:

```bash
docker compose ps
docker compose logs -f app
```

Если на сервере включен `ufw`, откройте порт:

```bash
sudo ufw allow 2222/tcp
```

После запуска и настройки DNS игроки подключаются так:

```bash
ssh -p 2222 smth.com
```

Порт `22` обычно занят обычным серверным SSH, поэтому игра по умолчанию слушает `2222`.

## Конфигурация

Основные переменные окружения:

```text
TB_SSH_PORT=2222
TB_HOST_KEY_PATH=/app/data/hostkey.ser
TB_DB_URL=jdbc:postgresql://postgres:5432/terminal_bang
TB_DB_USER=terminal_bang
TB_DB_PASSWORD=terminal_bang
TB_DB_POOL_SIZE=8
```

В `docker-compose.yml` поднимаются два сервиса: приложение и PostgreSQL.

## Архитектура

Стек:

- Java 21;
- Gradle;
- Apache MINA SSHD для embedded SSH-сервера;
- собственный ANSI/TUI renderer для терминального интерфейса;
- PostgreSQL для профилей, настроек, статистики, результатов матчей и справочников;
- Flyway для миграций;
- HikariCP для пула соединений;
- Docker Compose для запуска приложения и базы.

Ключевая идея решения: активные комнаты, матчи, таймеры и состояние хода живут в памяти процесса, потому что это mutable runtime-состояние игры. PostgreSQL хранит долговечные данные: аккаунты по SSH fingerprint, профили, язык интерфейса, историю матчей, статистику и определения карт/персонажей.

Основные зоны кода:

- `ssh` - SSH-сервер, fingerprint-идентификация и обработка подключений;
- `tui` - экраны, ввод, рендеринг, цвета и layout;
- `room` - создание комнат, public/private режим, lobby и старт матчей;
- `game` - состояние матча, правила, ходы, реакции, эффекты карт, победа;
- `persistence` - доступ к PostgreSQL;
- `stats` - сбор и сохранение результатов.

## Разработка

Запуск тестов:

```bash
./gradlew test --no-daemon
```

Локальный запуск без Docker требует доступный PostgreSQL и переменные окружения из секции конфигурации.
