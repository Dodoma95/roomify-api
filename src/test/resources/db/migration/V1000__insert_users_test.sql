-- Création d'un user de test
INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified)
VALUES (99999999998,
        'test.user@gmail.com',
        'Test',
        'User',
        '{bcrypt}Test@12345678941',
        false,
        false)
ON CONFLICT (email) DO NOTHING;

-- Attribution du rôle USER
INSERT INTO roomify.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM roomify.users u
         JOIN roomify.roles r ON r.name = 'USER'
WHERE u.email = 'test.user@gmail.com'
ON CONFLICT DO NOTHING;

INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified)
VALUES (99999999999,
        'test.admin@gmail.com',
        'Test',
        'Admin',
        '{bcrypt}Test@12345678941',
        true,
        true)
ON CONFLICT (email) DO NOTHING;

-- Création de l'utilisateur admin de test
INSERT INTO roomify.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM roomify.users u
         JOIN roomify.roles r ON r.name = 'ADMIN'
WHERE u.email = 'test.admin@gmail.com'
ON CONFLICT DO NOTHING;

-- Utilisateur owner de test (id fixe pour les tests de rôles)
INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified)
VALUES (99999999997,
        'test.owner@gmail.com',
        'Test',
        'Owner',
        '{bcrypt}Test@12345678941',
        true,
        true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO roomify.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM roomify.users u
         JOIN roomify.roles r ON r.name = 'OWNER'
WHERE u.email = 'test.owner@gmail.com'
ON CONFLICT DO NOTHING;

-- Utilisateur super admin de test (id fixe pour les tests de rôles)
INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified)
VALUES (99999999996,
        'test.super.admin@gmail.com',
        'Test',
        'SuperAdmin',
        '{bcrypt}Test@12345678941',
        true,
        true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO roomify.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM roomify.users u
         JOIN roomify.roles r ON r.name = 'SUPER_ADMIN'
WHERE u.email = 'test.super.admin@gmail.com'
ON CONFLICT DO NOTHING;