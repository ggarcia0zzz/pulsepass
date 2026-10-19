-- ============================================================================
-- PulsePass — Fixtures E2E (Fase 5). EXCLUSIVAMENTE para la base `pulsepass_e2e`.
--
-- Cuándo ejecutarlo: DESPUÉS de arrancar la aplicación al menos una vez
-- (Flyway crea las tablas V1–V4 en el arranque). Es idempotente.
--
-- Qué crea: los dos venues que la API no puede crear (la Fase 3 solo tiene GET):
--   VEN-E2E-01     capacidad 5000  -> venue activo para los eventos de prueba
--   VEN-E2E-SMALL  capacidad 1     -> para probar «aforo agotado» / SOLD_OUT
--
-- Qué NO crea (a propósito):
--   * Cuentas ADMIN/STAFF: las crea la semilla dev de la app (BootstrapAccountsRunner)
--     con contraseñas por variable de entorno. No hay hashes ni contraseñas aquí.
--   * Artistas: ya vienen de la migración V2 (Solar Beat, Neon Waves, ...).
--   * Usuarios USER_A/B/C/D, eventos y tickets: se crean por la propia API.
-- ============================================================================

-- Guardia: aborta si no estás en la base E2E (evita tocar desarrollo/QA/producción).
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Base equivocada: % (se esperaba pulsepass_e2e). No se ejecuta nada.', current_database();
    END IF;
END $$;

INSERT INTO venues (code, name, city, address, capacity, active) VALUES
    ('VEN-E2E-01',    'Estadio E2E',       'Santa Marta', 'Calle 1 # 2-3', 5000, TRUE),
    ('VEN-E2E-SMALL', 'Sala E2E Aforo 1',  'Santa Marta', 'Calle 4 # 5-6',    1, TRUE)
ON CONFLICT (code) DO NOTHING;

SELECT code, name, capacity, active FROM venues WHERE code LIKE 'VEN-E2E-%' ORDER BY code;
