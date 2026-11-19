-- Fase 4: credenciales y rol. Nullable/por defecto para no romper filas existentes;
-- un usuario sin password_hash no puede iniciar sesion.
ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100) NULL,
    ADD COLUMN role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'));

-- El telefono pasa a ser opcional (el registro publico no lo exige).
ALTER TABLE user_profiles ALTER COLUMN phone DROP NOT NULL;
