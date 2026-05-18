INSERT INTO card_definitions (id, name, type, description) VALUES
('shot', 'Shot', 'ATTACK', 'Attack a player in range. The target may answer with Dodge.'),
('dodge', 'Dodge', 'REACTION', 'Avoid one Shot.'),
('saloon', 'Saloon', 'UTILITY', 'Restore one health, up to the character maximum.'),
('trail_ride', 'Trail Ride', 'UTILITY', 'Draw two cards.'),
('disarm', 'Disarm', 'UTILITY', 'Force a target player to discard one random card.'),
('rustle', 'Rustle', 'UTILITY', 'Take one random card from a target in range.'),
('standoff', 'Standoff', 'ATTACK', 'Start a duel. Players answer with Shot until one fails.');

INSERT INTO character_definitions (id, name, max_health, description) VALUES
('marshal', 'Marshal', 4, 'Balanced starter character.'),
('scout', 'Scout', 4, 'Balanced starter character.'),
('gambler', 'Gambler', 4, 'Balanced starter character.'),
('rancher', 'Rancher', 4, 'Balanced starter character.'),
('doctor', 'Doctor', 4, 'Balanced starter character.'),
('tracker', 'Tracker', 4, 'Balanced starter character.'),
('blacksmith', 'Blacksmith', 4, 'Balanced starter character.');

