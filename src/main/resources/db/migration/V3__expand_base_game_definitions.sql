INSERT INTO card_definitions (id, name, type, description) VALUES
('bang', 'Bang!', 'BROWN', 'Attack a player within weapon range. The target may answer with Missed.'),
('missed', 'Missed!', 'REACTION', 'Cancel one Bang-style attack aimed at you.'),
('beer', 'Beer', 'BROWN', 'Recover one life point. It has no effect with only two players alive.'),
('cat_balou', 'Cat Balou', 'BROWN', 'Force any one player to discard one random hand or in-play card.'),
('duel', 'Duel', 'BROWN', 'Challenge any player. Players alternate Bang responses until one fails and loses one life.'),
('gatling', 'Gatling', 'BROWN', 'All other players must answer with Missed or lose one life.'),
('general_store', 'General Store', 'BROWN', 'Reveal one card per alive player; players receive one in turn order.'),
('indians', 'Indians!', 'BROWN', 'All other players must discard Bang or lose one life. Missed and Barrel do not help.'),
('panic', 'Panic!', 'BROWN', 'Take one random hand or in-play card from a player at distance 1.'),
('saloon', 'Saloon', 'BROWN', 'Every alive player recovers one life point.'),
('stagecoach', 'Stagecoach', 'BROWN', 'Draw two cards.'),
('wells_fargo', 'Wells Fargo', 'BROWN', 'Draw three cards.'),
('barrel', 'Barrel', 'BLUE', 'In play: when answering a Bang-style attack, draw! hearts count as one Missed.'),
('dynamite', 'Dynamite', 'BLUE', 'In play: at turn start, draw! spades 2-9 explodes for 3 damage; otherwise pass it left.'),
('jail', 'Jail', 'BLUE', 'Play in front of another non-Sheriff. At turn start, hearts escape; otherwise skip turn.'),
('mustang', 'Mustang', 'BLUE', 'In play: other players see you one distance farther away.'),
('scope', 'Scope', 'BLUE', 'In play: you see every other player one distance closer.'),
('remington', 'Remington', 'WEAPON', 'Weapon range 3.'),
('rev_carabine', 'Rev. Carabine', 'WEAPON', 'Weapon range 4.'),
('schofield', 'Schofield', 'WEAPON', 'Weapon range 2.'),
('volcanic', 'Volcanic', 'WEAPON', 'Weapon range 1, but you may play any number of Bang cards.'),
('winchester', 'Winchester', 'WEAPON', 'Weapon range 5.')
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
type = EXCLUDED.type,
description = EXCLUDED.description;

INSERT INTO character_definitions (id, name, max_health, description) VALUES
('bart_cassidy', 'Bart Cassidy', 4, 'Draws one card each time he loses a life point.'),
('black_jack', 'Black Jack', 4, 'Reveals his second draw; if it is red, he draws one extra card.'),
('calamity_janet', 'Calamity Janet', 4, 'Can use Bang as Missed and Missed as Bang.'),
('el_gringo', 'El Gringo', 3, 'When another player''s card hurts him, he takes random cards from that player''s hand.'),
('jesse_jones', 'Jesse Jones', 4, 'At draw phase, may take the first card from another player''s hand.'),
('jourdonnais', 'Jourdonnais', 4, 'Always has an extra Barrel check.'),
('kit_carlson', 'Kit Carlson', 4, 'Looks at three draw cards and keeps two.'),
('lucky_duke', 'Lucky Duke', 4, 'Draw! checks flip two cards and use the better result.'),
('paul_regret', 'Paul Regret', 3, 'Other players see him one distance farther away.'),
('pedro_ramirez', 'Pedro Ramirez', 4, 'May draw his first card from the discard pile.'),
('rose_doolan', 'Rose Doolan', 4, 'Sees every other player one distance closer.'),
('sid_ketchum', 'Sid Ketchum', 4, 'Can discard two cards to recover one life point.'),
('slab_the_killer', 'Slab the Killer', 4, 'His Bang cards require two Missed effects to cancel.'),
('suzy_lafayette', 'Suzy Lafayette', 4, 'Whenever her hand becomes empty, she draws one card.'),
('vulture_sam', 'Vulture Sam', 4, 'Takes the hand and in-play cards of eliminated players.'),
('willy_the_kid', 'Willy the Kid', 4, 'Can play any number of Bang cards during his turn.')
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
max_health = EXCLUDED.max_health,
description = EXCLUDED.description;
