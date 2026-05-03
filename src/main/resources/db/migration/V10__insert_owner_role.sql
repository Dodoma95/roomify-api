INSERT INTO roomify.roles (id, name)
VALUES (nextval('roomify.role_seq'), 'OWNER')
ON CONFLICT (name) DO NOTHING;