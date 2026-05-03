INSERT INTO IWUser (id, enabled, roles, username, password, num_victorias, num_derrotas, chat_ban,
    draw_two_played, draw_four_played, draw_two_drawn, draw_four_drawn, one_card_moments)
VALUES (1, TRUE, 'ADMIN,USER', 'a',
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 0, 0, FALSE,
    0, 0, 0, 0, 0);

INSERT INTO IWUser (id, enabled, roles, username, password, num_victorias, num_derrotas, chat_ban,
    draw_two_played, draw_four_played, draw_two_drawn, draw_four_drawn, one_card_moments)
VALUES (2, TRUE, 'USER', 'b',
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 0, 0, FALSE,
    0, 0, 0, 0, 0);

INSERT INTO IWUser (id, enabled, roles, username, password, num_victorias, num_derrotas, chat_ban,
    draw_two_played, draw_four_played, draw_two_drawn, draw_four_drawn, one_card_moments)
VALUES (3, TRUE, 'USER', 'c',
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 0, 0, FALSE,
    0, 0, 0, 0, 0);

INSERT INTO IWUser (id, enabled, roles, username, password, num_victorias, num_derrotas, chat_ban,
    draw_two_played, draw_four_played, draw_two_drawn, draw_four_drawn, one_card_moments)
VALUES (4, TRUE, 'USER', 'd',
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 0, 0, FALSE,
    0, 0, 0, 0, 0);

    UPDATE IWUser
SET num_victorias = 6,
    num_derrotas = 2,
    draw_two_played = 5,
    draw_four_played = 1,
    draw_two_drawn = 3,
    draw_four_drawn = 1,
    one_card_moments = 4
WHERE username = 'a';

UPDATE IWUser
SET num_victorias = 3,
    num_derrotas = 5,
    draw_two_played = 4,
    draw_four_played = 2,
    draw_two_drawn = 2,
    draw_four_drawn = 0,
    one_card_moments = 2
WHERE username = 'b';

INSERT INTO IWFriendship (id, player1_id, player2_id, games_played, times_betrayed, affinity_score)
SELECT 1, ua.id, ub.id, 7, 1, 8
FROM IWUser ua, IWUser ub
WHERE ua.username = 'a' AND ub.username = 'b';

INSERT INTO IWFriendship (id, player1_id, player2_id, games_played, times_betrayed, affinity_score)
SELECT 2, ua.id, uc.id, 4, 0, 5
FROM IWUser ua, IWUser uc
WHERE ua.username = 'a' AND uc.username = 'c';

INSERT INTO IWFriendship (id, player1_id, player2_id, games_played, times_betrayed, affinity_score)
SELECT 3, ub.id, uc.id, 9, 2, 6
FROM IWUser ub, IWUser uc
WHERE ub.username = 'b' AND uc.username = 'c';

ALTER SEQUENCE gen RESTART WITH 5;