UPDATE card_definitions
SET description = 'Recover one life point. Cannot be played at full health and has no effect with only two players alive.'
WHERE id = 'beer';

UPDATE card_definitions
SET description = 'Reveal one card per alive player; each player chooses one card in turn order.'
WHERE id = 'general_store';
