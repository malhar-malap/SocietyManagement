-- Seed Users and Rooms (Idempotent using INSERT IGNORE / NOT EXISTS)

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'admin@jaswanti.com', 1, 1, 'Admin', '$2a$10$DAmzyQm0rGLPdbw9BZ70B.6qyhhTH5jQ9ez3ZAKaI6U.6ZhGnpKXO', 'ADMIN', 'ACTIVE', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@jaswanti.com');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident101@jaswanti.com', 0, 1, 'Resident 101', '$2a$10$9r75d7y/YRmqfiNFF4SFdOoHuknNcBEPme6HR5xbgvSJZhOdJ.lhq', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident101@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-101', 1, 'C', id
FROM users WHERE email = 'resident101@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-101');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident102@jaswanti.com', 0, 1, 'Resident 102', '$2a$10$kEhqjhyCxdihATo5KGeN0O7a1l2EtCcQMpXvSi9uygMBq23yB8kf2', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident102@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-102', 1, 'C', id
FROM users WHERE email = 'resident102@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-102');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident103@jaswanti.com', 0, 1, 'Resident 103', '$2a$10$t0tWIoRfcTBNuiU7cRjV5O9.7XVGwgXQO465nFso6nETZqFVbqPny', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident103@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-103', 1, 'C', id
FROM users WHERE email = 'resident103@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-103');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident104@jaswanti.com', 0, 1, 'Resident 104', '$2a$10$j.2q098WCzu6Paqn5hjQ1etDX5Rljh98N3ugEWO3e9CATNFlzNMMi', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident104@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-104', 1, 'C', id
FROM users WHERE email = 'resident104@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-104');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident105@jaswanti.com', 0, 1, 'Resident 105', '$2a$10$L4t4WY6Te8E.5.t7aBCMmuqqCzJlTzRxYboApEa8NRA7RzVcHNZIu', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident105@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-105', 1, 'C', id
FROM users WHERE email = 'resident105@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-105');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident106@jaswanti.com', 0, 1, 'Resident 106', '$2a$10$04DcUH8w408oeTNU5hdkpusUuGd1AIt5S.7WWTc74pgpYSZ2xSNqm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident106@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-106', 1, 'C', id
FROM users WHERE email = 'resident106@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-106');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident107@jaswanti.com', 0, 1, 'Resident 107', '$2a$10$eME1bDJntrgYvNvAPofJzeSs/fOPaF42EroqxuOJc73UmeQUxK8Hm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident107@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-107', 1, 'C', id
FROM users WHERE email = 'resident107@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-107');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident108@jaswanti.com', 0, 1, 'Resident 108', '$2a$10$k5blTG19IrEOxdbCqFhyq.g2vePFDiy3tBm/2e9n1mqp51mRMkX7C', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident108@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-108', 1, 'C', id
FROM users WHERE email = 'resident108@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-108');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident201@jaswanti.com', 0, 1, 'Resident 201', '$2a$10$tuA/0iE10.8/JmJdayoH.ey5U.DZJD3lLy9XD09U7wOs9T.ukzjs.', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident201@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-201', 2, 'C', id
FROM users WHERE email = 'resident201@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-201');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident202@jaswanti.com', 0, 1, 'Resident 202', '$2a$10$rxGO6iMsEMCi8Qjqsh47GOuwX10aIk.jqIEucJPY4BdKs3ggZkjgy', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident202@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-202', 2, 'C', id
FROM users WHERE email = 'resident202@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-202');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident203@jaswanti.com', 0, 1, 'Resident 203', '$2a$10$10jTLZtGPlkWf9u1GhGgMuzROgMfI54mQdzeG8bIS3/wrFCJ9N/Rq', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident203@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-203', 2, 'C', id
FROM users WHERE email = 'resident203@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-203');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident204@jaswanti.com', 0, 1, 'Resident 204', '$2a$10$HkpZ0Hi0Oe4PsCDSBQd6SuLgbhTY3kFs1yd9QSbaD6hgcG7jga1zy', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident204@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-204', 2, 'C', id
FROM users WHERE email = 'resident204@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-204');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident205@jaswanti.com', 0, 1, 'Resident 205', '$2a$10$VzVwM.U9cjRuFShqo3/h0eZr5fCwtC1yVjGA64XdiUFeJCKC9Fmw2', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident205@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-205', 2, 'C', id
FROM users WHERE email = 'resident205@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-205');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident206@jaswanti.com', 0, 1, 'Resident 206', '$2a$10$6mGXyJqTxJGJ2.B4HCDK6u.4g.qeg5nFYllHcQhiFm85eKlruAI4.', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident206@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-206', 2, 'C', id
FROM users WHERE email = 'resident206@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-206');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident207@jaswanti.com', 0, 1, 'Resident 207', '$2a$10$CkTf6E2SxGVwPZLlw1qVlOMfMZr5DnS5EszvaDqDRscrgNZCjwinK', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident207@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-207', 2, 'C', id
FROM users WHERE email = 'resident207@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-207');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident208@jaswanti.com', 0, 1, 'Resident 208', '$2a$10$x/Y7X50QKpNmqD39uSvL/uLCAfrRn3KrayAKShXo2yTkQLV8QhOMK', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident208@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-208', 2, 'C', id
FROM users WHERE email = 'resident208@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-208');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident301@jaswanti.com', 0, 1, 'Resident 301', '$2a$10$eKnQ3WAkDUtmipyKpQZ5NOO0BsCiZ6FlV7/ExMYNlNoOql.JH6Mey', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident301@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-301', 3, 'C', id
FROM users WHERE email = 'resident301@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-301');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident302@jaswanti.com', 0, 1, 'Resident 302', '$2a$10$OWlpk7.Uo18N9PChIV5Hp.8cGpLxYZWq6LiVmsOIUIwdssgW1M722', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident302@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-302', 3, 'C', id
FROM users WHERE email = 'resident302@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-302');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident303@jaswanti.com', 0, 1, 'Resident 303', '$2a$10$MFMsmI2UqgkzlE.APoTaButUAZBrPyHmzb3hMyKcz6iotPUe6g8em', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident303@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-303', 3, 'C', id
FROM users WHERE email = 'resident303@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-303');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident304@jaswanti.com', 0, 1, 'Resident 304', '$2a$10$CdbTZ3xI.dcmPBDFRCUdGer3Df59gFpPLM1us7m4LXIybKCOiPXku', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident304@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-304', 3, 'C', id
FROM users WHERE email = 'resident304@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-304');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident305@jaswanti.com', 0, 1, 'Resident 305', '$2a$10$4Ri/uQktWfqcd3TgAbsPvOC7oYkPHncdvUXCvIRfGuc.4uTl3eARm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident305@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-305', 3, 'C', id
FROM users WHERE email = 'resident305@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-305');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident306@jaswanti.com', 0, 1, 'Resident 306', '$2a$10$TP9wHlkN/DeX6.PyR5qI3elzsoxkIh0qoaki3KRehp5cCkgahkKG2', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident306@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-306', 3, 'C', id
FROM users WHERE email = 'resident306@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-306');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident307@jaswanti.com', 0, 1, 'Resident 307', '$2a$10$PQy1tS8DGdXGb5gKiY2PWO9mebsspdjUux3lbAC5LMbNcQozllBYu', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident307@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-307', 3, 'C', id
FROM users WHERE email = 'resident307@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-307');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident308@jaswanti.com', 0, 1, 'Resident 308', '$2a$10$whGuWiOgj6CgGJE9ArAs..iFtixHr4JE7tKMS9b6nk2y3gzuixCbO', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident308@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-308', 3, 'C', id
FROM users WHERE email = 'resident308@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-308');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident401@jaswanti.com', 0, 1, 'Resident 401', '$2a$10$W3mpEH9fiXSNQQ3IOqVNsuasxDVVq0/Qy.3fSarEpf5DoGZBQgCxi', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident401@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-401', 4, 'C', id
FROM users WHERE email = 'resident401@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-401');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident402@jaswanti.com', 0, 1, 'Resident 402', '$2a$10$Ez/k67zKM8Sw.BzSNV3Wa.ixaP7GFJ7gzWzgfWsKJEasH/rxZ7kPO', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident402@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-402', 4, 'C', id
FROM users WHERE email = 'resident402@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-402');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident403@jaswanti.com', 0, 1, 'Resident 403', '$2a$10$25NbFDK6uKL8MS40HdivH.zg/DH4Ud87pwEdOYl2v0VZmESXbIa92', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident403@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-403', 4, 'C', id
FROM users WHERE email = 'resident403@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-403');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident404@jaswanti.com', 0, 1, 'Resident 404', '$2a$10$R3leyOGxeNMHzGtmeeajJuLWlLBEHqTwmizlrwQpPm191UiJop636', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident404@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-404', 4, 'C', id
FROM users WHERE email = 'resident404@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-404');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident405@jaswanti.com', 0, 1, 'Resident 405', '$2a$10$5b5ZGK1Uj3buTl5H/sIzrO1YFOays2ahLGZan1rq9aBQ2Rs8A2Gni', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident405@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-405', 4, 'C', id
FROM users WHERE email = 'resident405@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-405');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident406@jaswanti.com', 0, 1, 'Resident 406', '$2a$10$yjwptMqML/mgY7h7wR2a5uu4VBg261JVYxASjSFTuiKA/jcfnjaKq', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident406@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-406', 4, 'C', id
FROM users WHERE email = 'resident406@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-406');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident407@jaswanti.com', 0, 1, 'Resident 407', '$2a$10$BYzoAeiRCr3hj0NF3mReguTlf4B6t29rSkJ6qFKbupKysMCmNdVri', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident407@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-407', 4, 'C', id
FROM users WHERE email = 'resident407@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-407');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident408@jaswanti.com', 0, 1, 'Resident 408', '$2a$10$vwZbb26S6.eKLlw6/MSUmu5C0pZCsM8WdsKwG7rdTL7D1dPZpB5lS', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident408@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-408', 4, 'C', id
FROM users WHERE email = 'resident408@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-408');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident501@jaswanti.com', 0, 1, 'Resident 501', '$2a$10$6/4W6DK2kBb2cXbD.pC.VeJF33Jiw1cTACl7/Ga.p08jy4aGhLwLm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident501@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-501', 5, 'C', id
FROM users WHERE email = 'resident501@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-501');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident502@jaswanti.com', 0, 1, 'Resident 502', '$2a$10$BcYv0QoyFho1jsBMxyWJ5OQYV77B9sL1IixkQj8TJ.ZuWG3hKYLzW', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident502@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-502', 5, 'C', id
FROM users WHERE email = 'resident502@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-502');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident503@jaswanti.com', 0, 1, 'Resident 503', '$2a$10$ehiGDBUFlmoMsNzXOYxqz.kfzHKzmx9h0ln9TYmL4DYd.2f5/G32y', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident503@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-503', 5, 'C', id
FROM users WHERE email = 'resident503@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-503');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident504@jaswanti.com', 0, 1, 'Resident 504', '$2a$10$BBJel9/uBSstMXwVXI0L5uf9y9BAOHD95pUp.Y6gr9yGlJpVXMQz2', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident504@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-504', 5, 'C', id
FROM users WHERE email = 'resident504@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-504');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident505@jaswanti.com', 0, 1, 'Resident 505', '$2a$10$vctTr2OG8luplRpIut7TtODI1yIZirWL93rTc.IBdMAuR4afBt4xa', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident505@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-505', 5, 'C', id
FROM users WHERE email = 'resident505@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-505');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident506@jaswanti.com', 0, 1, 'Resident 506', '$2a$10$JDF0DWyG79PogIb.0lQEkupLkpFsICbqPowWSriP7lVoF6ZKVX98a', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident506@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-506', 5, 'C', id
FROM users WHERE email = 'resident506@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-506');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident507@jaswanti.com', 0, 1, 'Resident 507', '$2a$10$NrRE0s1pV1xMuhOYLvSrReg3xDgrTjL2vKLgoeILmxNL17dsuH7qG', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident507@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-507', 5, 'C', id
FROM users WHERE email = 'resident507@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-507');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident508@jaswanti.com', 0, 1, 'Resident 508', '$2a$10$1/MQGKQHGsME/U7jzvxKo.BVedMxbN6APL0xjR1EgH/54lRoEbRke', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident508@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-508', 5, 'C', id
FROM users WHERE email = 'resident508@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-508');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident601@jaswanti.com', 0, 1, 'Resident 601', '$2a$10$gcYJLaZXd5aKVPJ0NAwc6.MS6INUHt5mrTEA6uQhy4dSj4AtDazcO', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident601@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-601', 6, 'C', id
FROM users WHERE email = 'resident601@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-601');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident602@jaswanti.com', 0, 1, 'Resident 602', '$2a$10$zaG2epASBBfSiKqZVCoSYufp3buFJNjVjI5d2H7yy3f/WvqabSQB2', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident602@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-602', 6, 'C', id
FROM users WHERE email = 'resident602@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-602');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident603@jaswanti.com', 0, 1, 'Resident 603', '$2a$10$MWEQ8eicF/CmZfO/rwSp5eetmwxWweK/bZrrAH6tw6.FHX20uYeXm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident603@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-603', 6, 'C', id
FROM users WHERE email = 'resident603@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-603');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident604@jaswanti.com', 0, 1, 'Resident 604', '$2a$10$OY4D1oPqRlczNsQM3woayOa/5efgzfkFOakYdeynl2XZC2vPlHLnC', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident604@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-604', 6, 'C', id
FROM users WHERE email = 'resident604@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-604');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident605@jaswanti.com', 0, 1, 'Resident 605', '$2a$10$VFApAWBi.KY70YnApjtf3ujhdHkOllwdHxNpjq4EjBHoNjfVv96PK', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident605@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-605', 6, 'C', id
FROM users WHERE email = 'resident605@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-605');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident606@jaswanti.com', 0, 1, 'Resident 606', '$2a$10$mYE.uXTL8rSuigIo1T.0yuOh7WE7Pr84RnMTX.nbPPFk5cvyRJKyO', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident606@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-606', 6, 'C', id
FROM users WHERE email = 'resident606@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-606');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident607@jaswanti.com', 0, 1, 'Resident 607', '$2a$10$EDi/cpG.ZWEXtFWXz8bOceETDHAzSs8Ci2bvLL56nSvYJoBCUZTp2', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident607@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-607', 6, 'C', id
FROM users WHERE email = 'resident607@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-607');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident608@jaswanti.com', 0, 1, 'Resident 608', '$2a$10$JYEC8oOzQc4QKE28/DtNjuonyhPfARWS5XugF.3u6sfuflFQj60tm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident608@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-608', 6, 'C', id
FROM users WHERE email = 'resident608@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-608');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident701@jaswanti.com', 0, 1, 'Resident 701', '$2a$10$MzjY41t.Xttnco88CNwivuFKr4N5ofBFF6EPUFbDQZP1CKBcefExO', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident701@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-701', 7, 'C', id
FROM users WHERE email = 'resident701@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-701');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident702@jaswanti.com', 0, 1, 'Resident 702', '$2a$10$20kStIAIWJmhcO489AqGH.nrf6k2v3w6QW3izJkaQ0xKJfhDxyhOW', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident702@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-702', 7, 'C', id
FROM users WHERE email = 'resident702@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-702');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident703@jaswanti.com', 0, 1, 'Resident 703', '$2a$10$f5fi/PQqM9bPTUUgswdn5ex0w55caAw6VyFLkLbRoENu7KOih8yaK', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident703@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-703', 7, 'C', id
FROM users WHERE email = 'resident703@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-703');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident704@jaswanti.com', 0, 1, 'Resident 704', '$2a$10$H5eV3amyKrpqSE8sLPDaYOfZ8Wz3etHT7v6m9ILayLrj7dYq0y55S', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident704@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-704', 7, 'C', id
FROM users WHERE email = 'resident704@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-704');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident705@jaswanti.com', 0, 1, 'Resident 705', '$2a$10$FCEHVUPM.iJ0HrdXykqVSOtDnV7VT/GLFaMjPOSKvieSzn9loEa4C', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident705@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-705', 7, 'C', id
FROM users WHERE email = 'resident705@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-705');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident706@jaswanti.com', 0, 1, 'Resident 706', '$2a$10$pMciObq48EsK8f16bsS4jucPkGY1ucRgpiJ.9Z1TPTuy8ziCYFwGG', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident706@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-706', 7, 'C', id
FROM users WHERE email = 'resident706@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-706');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident707@jaswanti.com', 0, 1, 'Resident 707', '$2a$10$jdqgIfN6bJlejRLOHmRe7eiMwgByGCwi.iIt1ZX02Lt3xJxkEs9aC', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident707@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-707', 7, 'C', id
FROM users WHERE email = 'resident707@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-707');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident708@jaswanti.com', 0, 1, 'Resident 708', '$2a$10$kTBwz4lnxnC1taZV6usECuEk/NCQSSmukbjva8//Yi.fT32QUQJdm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident708@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-708', 7, 'C', id
FROM users WHERE email = 'resident708@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-708');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident801@jaswanti.com', 0, 1, 'Resident 801', '$2a$10$TPH7uqj/DEh9//3kuu2sMOnxFvPkuFmx9WLC9ej7HzmjMPqUlm2Ie', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident801@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-801', 8, 'C', id
FROM users WHERE email = 'resident801@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-801');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident802@jaswanti.com', 0, 1, 'Resident 802', '$2a$10$pKry.yYi7/nEDo7sWiUv9OLJDCCnb8xsx.W8eituKF8y0H889L/DK', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident802@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-802', 8, 'C', id
FROM users WHERE email = 'resident802@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-802');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident803@jaswanti.com', 0, 1, 'Resident 803', '$2a$10$mMdnNbYY0E4uatKCNBvjJOHrH.iYApBGa2Eh7qy1nZDmcylH/LxsC', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident803@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-803', 8, 'C', id
FROM users WHERE email = 'resident803@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-803');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident804@jaswanti.com', 0, 1, 'Resident 804', '$2a$10$2Zkzn8Dq69iZ/onWc9Pvku8khy1D5hwc0j2/zdJLiFfjBDEF4VXyS', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident804@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-804', 8, 'C', id
FROM users WHERE email = 'resident804@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-804');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident805@jaswanti.com', 0, 1, 'Resident 805', '$2a$10$Hg4WVaigRCPotqlqj5Zz/eEh1VL68m8jjh0witMKJstmLv9FzzqzO', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident805@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-805', 8, 'C', id
FROM users WHERE email = 'resident805@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-805');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident806@jaswanti.com', 0, 1, 'Resident 806', '$2a$10$e0foTrAqKhHvU6BzvpigzONVzHfvWdqU4Ixw6zICNfRXj0t1p1sDW', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident806@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-806', 8, 'C', id
FROM users WHERE email = 'resident806@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-806');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident807@jaswanti.com', 0, 1, 'Resident 807', '$2a$10$h.HsUy5Ote2nND5CkSx2FODvnWJZlkWelZk0gmsLg5ovyg2c6uPLy', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident807@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-807', 8, 'C', id
FROM users WHERE email = 'resident807@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-807');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident808@jaswanti.com', 0, 1, 'Resident 808', '$2a$10$ot8WGtGya94WpXqjAgCRm.YNurptEqZy/FZNjz9bTDcpKBzmIPGri', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident808@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-808', 8, 'C', id
FROM users WHERE email = 'resident808@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-808');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident901@jaswanti.com', 0, 1, 'Resident 901', '$2a$10$m/UI63PMH3Dll4oUnkqRxOdDEqnL8EW/LzS17Gkjfa/wwCwebnFj.', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident901@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-901', 9, 'C', id
FROM users WHERE email = 'resident901@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-901');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident902@jaswanti.com', 0, 1, 'Resident 902', '$2a$10$D5WH/Mz5VWbjb96ZA0psYeL8G7jKHKqryv./6VzNGr1txvo65FNYS', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident902@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-902', 9, 'C', id
FROM users WHERE email = 'resident902@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-902');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident903@jaswanti.com', 0, 1, 'Resident 903', '$2a$10$K5Pvvy46w/R7y/gee4XOjuvcUqPLOIg1cTayn6hTmRpmQeQQK8SiC', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident903@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-903', 9, 'C', id
FROM users WHERE email = 'resident903@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-903');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident904@jaswanti.com', 0, 1, 'Resident 904', '$2a$10$LDPhj.ZQFrkgZkTndkJ8l.8Z5AFaoYFRb8TtUe2//IYvfQULc9wN.', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident904@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-904', 9, 'C', id
FROM users WHERE email = 'resident904@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-904');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident905@jaswanti.com', 0, 1, 'Resident 905', '$2a$10$6R0H9HFPNcy5znwR8pcwnuoECfJk9nbdoqNfxun8pb2ObBQFU5ba2', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident905@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-905', 9, 'C', id
FROM users WHERE email = 'resident905@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-905');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident906@jaswanti.com', 0, 1, 'Resident 906', '$2a$10$yvue6PasCDjY.oHveCEffe1MHU9mpXBAlsuXspbxEZt/rywx4UZKC', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident906@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-906', 9, 'C', id
FROM users WHERE email = 'resident906@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-906');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident907@jaswanti.com', 0, 1, 'Resident 907', '$2a$10$F7uKXUbllPNoQWSw4s066.qua6rHQkW88Fbj2ehZhMMfRr/LY5.zq', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident907@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-907', 9, 'C', id
FROM users WHERE email = 'resident907@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-907');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident908@jaswanti.com', 0, 1, 'Resident 908', '$2a$10$WKxbaM1KZSWT8/OKHPVYPuUsIM9HhtNPLCHgZy.re/tAwOeLOtXE.', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident908@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-908', 9, 'C', id
FROM users WHERE email = 'resident908@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-908');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1001@jaswanti.com', 0, 1, 'Resident 1001', '$2a$10$btyJQQngWr6iVa7lwNu.au8O3MqFAN8K6vrMgS6cqCVwWOYMm419m', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1001@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1001', 10, 'C', id
FROM users WHERE email = 'resident1001@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1001');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1002@jaswanti.com', 0, 1, 'Resident 1002', '$2a$10$yeMYP83i6u18PrEk74mRv.EPQmjLNufx8Zgf2.X7mqoQA3OWr6gHu', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1002@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1002', 10, 'C', id
FROM users WHERE email = 'resident1002@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1002');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1003@jaswanti.com', 0, 1, 'Resident 1003', '$2a$10$5MWd9FIg7yw.s01PSqdIKeWXCT30ttGW8Hl9fRR3Oj/gzYBFxNPxi', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1003@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1003', 10, 'C', id
FROM users WHERE email = 'resident1003@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1003');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1004@jaswanti.com', 0, 1, 'Resident 1004', '$2a$10$i/m6A3srSS7NCS11nt7Kz.cYWokG71sYGz.FmsY8Tlx4vlwrewTjG', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1004@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1004', 10, 'C', id
FROM users WHERE email = 'resident1004@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1004');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1005@jaswanti.com', 0, 1, 'Resident 1005', '$2a$10$bFuERlT25OtELnH9T/pvHuSQUgbGhe1hKA5OyLcp/QscB95VjY/o.', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1005@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1005', 10, 'C', id
FROM users WHERE email = 'resident1005@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1005');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1006@jaswanti.com', 0, 1, 'Resident 1006', '$2a$10$nuQzq3W7dpkBn2Cp32.MpunSQVfwkKGLnj1UgDaFeAmx03ExWuFzO', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1006@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1006', 10, 'C', id
FROM users WHERE email = 'resident1006@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1006');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1007@jaswanti.com', 0, 1, 'Resident 1007', '$2a$10$MlEAbJz9NLf5ALRA6OarrOXyC.okgUsR14xoVxje44beWkGxcvFtm', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1007@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1007', 10, 'C', id
FROM users WHERE email = 'resident1007@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1007');

INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)
SELECT 'resident1008@jaswanti.com', 0, 1, 'Resident 1008', '$2a$10$m94zi1DT9FdMYsvJWQjIB.W/qFqMVVFg.BfYETz6fPNZHKOB7InHu', 'RESIDENT', 'ACTIVE', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'resident1008@jaswanti.com');
INSERT INTO rooms (room_number, floor, wing, resident_id)
SELECT 'C-1008', 10, 'C', id
FROM users WHERE email = 'resident1008@jaswanti.com'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = 'C-1008');

