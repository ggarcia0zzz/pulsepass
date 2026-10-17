-- Fase 4: credenciales y roles.
-- Los usuarios preexistentes NO reciben contraseña: quedan sin posibilidad de login
-- hasta una activación segura (no se inventa ninguna contraseña por defecto).

CREATE TABLE user_credentials (
    user_id              BIGINT       NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    password_changed_at  TIMESTAMP    NULL,

    CONSTRAINT pk_user_credentials PRIMARY KEY (user_id),
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_roles (
    user_id  BIGINT      NOT NULL,
    role     VARCHAR(20) NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

-- Estrategia para datos legados: todo usuario existente recibe solo ROLE_USER.
-- Los roles privilegiados (STAFF/ADMIN) se provisionan por proceso administrativo,
-- nunca por esta migración ni por el registro público.
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users;
