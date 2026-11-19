-- ============================================================================
-- SEED EXCLUSIVO PARA LA BASE DE PRUEBAS E2E (pulsepass_e2e). NUNCA en dev/QA/prod.
-- Las contrasenas NO estan en el repositorio: se pasan por variable al ejecutar.
--
--   psql -d pulsepass_e2e \
--        -v admin_pw='<password admin>' -v staff_pw='<password staff>' -v inactive_pw='<password inactivo>' \
--        -f seed-e2e.sql
--
-- Requiere la extension pgcrypto (crypt/gen_salt 'bf' genera hashes $2a$ que Spring BCrypt acepta).
-- Prerrequisito: la app ya arranco una vez contra esta BD (Flyway aplico V1..V5).
-- Los venues y artistas iniciales vienen de las migraciones V5 y V2.
-- ============================================================================
\set ON_ERROR_STOP on

DO $$ BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Este seed solo puede ejecutarse en pulsepass_e2e (actual: %)', current_database();
    END IF;
END $$;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Usuarios privilegiados e inactivo (el registro publico solo crea USER)
INSERT INTO users (username, email, active, password_hash, role) VALUES
    ('e2e_admin',    'admin@example.test',    TRUE,  crypt(:'admin_pw',    gen_salt('bf', 10)), 'ADMIN'),
    ('e2e_staff',    'staff@example.test',    TRUE,  crypt(:'staff_pw',    gen_salt('bf', 10)), 'STAFF'),
    ('e2e_inactive', 'inactive@example.test', FALSE, crypt(:'inactive_pw', gen_salt('bf', 10)), 'USER')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_profiles (first_name, last_name, city, phone, birth_date, user_id)
SELECT 'Admin', 'E2E', 'Santa Marta', NULL, DATE '1990-01-01', id FROM users WHERE username = 'e2e_admin'
ON CONFLICT (user_id) DO NOTHING;
INSERT INTO user_profiles (first_name, last_name, city, phone, birth_date, user_id)
SELECT 'Staff', 'E2E', 'Santa Marta', NULL, DATE '1992-01-01', id FROM users WHERE username = 'e2e_staff'
ON CONFLICT (user_id) DO NOTHING;
INSERT INTO user_profiles (first_name, last_name, city, phone, birth_date, user_id)
SELECT 'Inactivo', 'E2E', 'Santa Marta', NULL, DATE '1995-01-01', id FROM users WHERE username = 'e2e_inactive'
ON CONFLICT (user_id) DO NOTHING;

-- Eventos fixture con fecha siempre futura (hoy + 90 dias)
INSERT INTO events (event_code, name, description, category, status, event_date, venue_id, minimum_age)
SELECT c.code, c.name, 'Fixture E2E', 'MUSIC', c.status, now() + interval '90 days', v.id, c.min_age
FROM (VALUES
    ('E2E-DRAFT',     'E2E Borrador',        'DRAFT',     0),
    ('E2E-PUBLISHED', 'E2E Publicado',       'PUBLISHED', 0),
    ('E2E-TOPUBLISH', 'E2E Por publicar',    'DRAFT',     0),
    ('E2E-ADULTS',    'E2E Solo mayores',    'PUBLISHED', 99)
) AS c(code, name, status, min_age)
CROSS JOIN (SELECT id FROM venues WHERE code = 'SM-ARENA') v
ON CONFLICT (event_code) DO NOTHING;

-- Un artista asociado al evento publicado (para by-artist)
INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id FROM events e, artists a
WHERE e.event_code = 'E2E-PUBLISHED' AND a.stage_name = 'Solar Beat'
ON CONFLICT DO NOTHING;
