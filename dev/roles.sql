-- Ejecutar exclusivamente en la base local de desarrollo después de registrar las cuentas.
BEGIN;
UPDATE users SET role='ADMIN' WHERE email='admin@pulsepass.test' AND password_hash IS NOT NULL;
UPDATE users SET role='STAFF' WHERE email='staff@pulsepass.test' AND password_hash IS NOT NULL;
SELECT username,email,role,active FROM users WHERE email IN ('admin@pulsepass.test','staff@pulsepass.test','user@pulsepass.test');
COMMIT;
