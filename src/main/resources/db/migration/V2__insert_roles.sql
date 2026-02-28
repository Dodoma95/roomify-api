INSERT INTO roomify.roles (id, name)
VALUES (nextval('roomify.role_seq'), 'ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roomify.roles (id, name)
VALUES (nextval('roomify.role_seq'), 'USER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roomify.roles (id, name)
VALUES (nextval('roomify.role_seq'), 'SUPER_ADMIN')
ON CONFLICT (name) DO NOTHING;