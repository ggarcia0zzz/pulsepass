# Fixtures y operaciones SQL de la prueba E2E

> **Todo esto es solo para la base `pulsepass_e2e`.** Nunca lo ejecutes contra desarrollo, QA o producción.
> Antes de cada comando verifica la base: `SELECT current_database();`

## 1. Qué datos necesita la prueba y de dónde salen

| Fixture | Origen |
|---|---|
| Venue activo `VEN-E2E-01` y venue de aforo 1 `VEN-E2E-SMALL` | `seed-e2e.sql` (la API no puede crear venues) |
| Artistas (Solar Beat, ...) | Migración Flyway V2 |
| ADMIN y STAFF | Semilla dev de la app (`BootstrapAccountsRunner`), contraseña por variable de entorno, hash BCrypt generado en el arranque |
| USER_A, USER_B, USER_C (inactivo), USER_D | Registro público por la API (`POST /api/users`) |
| Eventos (publicado, borrador, edad 99, aforo 1) y tickets | Se crean por la API durante las pruebas |

No hay contraseñas ni hashes en este repositorio.

## 2. Cargar el seed

Con la aplicación ya arrancada (Flyway creó las tablas):

```powershell
psql -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
```

Debe imprimir dos filas: `VEN-E2E-01` y `VEN-E2E-SMALL`. Si ves «Base equivocada», no estás en `pulsepass_e2e`.

## 3. Operaciones manuales que piden los casos de seguridad

Cambia `userc-r01` por el valor real de `userCUsername` si cambiaste el `runId`.

### Desactivar usuario (TC-AUTH-013)
Ejecutar **después** de haber copiado `inactiveToken` (login de USER_C):

```sql
SELECT current_database();
UPDATE users SET active = FALSE WHERE username = 'userc-r01';
```

Reactivar (opcional, para repetir): `UPDATE users SET active = TRUE WHERE username = 'userc-r01';`

### Revocar rol (TC-SEC-017)
Conserva el `staffToken` que ya tenías y luego:

```sql
SELECT current_database();
DELETE FROM user_roles
WHERE role = 'STAFF'
  AND user_id = (SELECT id FROM users WHERE username = 'staff');
```

### Restaurar rol (obligatorio al terminar TC-SEC-017)

```sql
INSERT INTO user_roles (user_id, role)
SELECT id, 'STAFF' FROM users WHERE username = 'staff'
ON CONFLICT DO NOTHING;
```

## 4. Empezar de cero (restauración de la base E2E)

Antes de cada sesión se recomienda partir de datos limpios (así `GET /api/events/by-artist` y `/published` no arrastran eventos de otras corridas).

1. Detén la aplicación.
2. Recrea **solo** la base E2E:

```powershell
psql -U postgres -c "DROP DATABASE IF EXISTS pulsepass_e2e;" -c "CREATE DATABASE pulsepass_e2e;"
```

3. Arranca la aplicación (Flyway recrea el esquema y la semilla dev recrea admin/staff).
4. Ejecuta de nuevo `seed-e2e.sql`.

Alternativa sin borrar la base: cambia el `runId` y los valores del ambiente (ver README, sección 8). Ten en cuenta que los eventos de corridas anteriores siguen en la base.

> Si usas Docker para PostgreSQL, antepón `docker exec -it <contenedor> ` a los comandos `psql`.
