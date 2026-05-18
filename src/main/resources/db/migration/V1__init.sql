CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    ssh_fingerprint TEXT NOT NULL UNIQUE,
    nickname TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE match_results (
    id UUID PRIMARY KEY,
    room_code TEXT NOT NULL,
    status TEXT NOT NULL,
    winner TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE player_match_stats (
    match_id UUID NOT NULL REFERENCES match_results(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    nickname TEXT NOT NULL,
    role TEXT NOT NULL,
    won BOOLEAN NOT NULL,
    eliminated BOOLEAN NOT NULL,
    damage_dealt INT NOT NULL,
    damage_taken INT NOT NULL,
    cards_played INT NOT NULL,
    PRIMARY KEY (match_id, account_id)
);

CREATE TABLE card_definitions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE character_definitions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    max_health INT NOT NULL,
    description TEXT NOT NULL
);

