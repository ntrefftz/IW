-- insert admin (username a, password aa)
INSERT INTO IWUser (id, enabled, roles, username, password, num_victorias, num_derrotas, chat_ban)
VALUES (1, TRUE, 'ADMIN,USER', 'a',
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 0, 0, FALSE);
INSERT INTO IWUser (id, enabled, roles, username, password, num_victorias, num_derrotas, chat_ban)
VALUES (2, TRUE, 'USER', 'b',
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 0, 0, FALSE);

-- start id numbering from a value that is larger than any assigned above
ALTER SEQUENCE "PUBLIC"."GEN" RESTART WITH 1024;
