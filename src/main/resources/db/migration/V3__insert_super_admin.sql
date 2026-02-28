INSERT INTO roomify.users (id, email, password, enabled, email_verified)
VALUES (nextval('roomify.user_seq'),
        'super.admin@gmail.com',
        '{bcrypt}$2a$10$egycV/MNatN0H3hx3NnxEe0nx45IrBKGVESSU1c9.O0BqkKawhGFS',
        true,
        true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO roomify.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM roomify.users u
         JOIN roomify.roles r ON r.name = 'SUPER_ADMIN'
WHERE u.email = 'super.admin@gmail.com'
ON CONFLICT DO NOTHING;
