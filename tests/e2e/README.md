# PulsePass — Pruebas E2E manuales con Postman (Fase 5)

Guía para ejecutar, **a mano y solicitud por solicitud**, las 22 rutas de PulsePass (20 de negocio + `POST /api/auth/login` + `GET /api/auth/me`) contra la aplicación real y su PostgreSQL. Sin mocks, sin scripts de Postman, sin Runner, sin Newman.

> Esto es una guía de ejecución, **no un reporte de resultados**: nada aquí asegura que la API funcione. Los resultados se anotan en `registro-ejecucion-manual.md`.

## 1. Qué contiene esta carpeta

| Archivo | Para qué sirve |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | Colección v2.1: 131 solicitudes en 10 carpetas, sin scripts |
| `postman/PulsePass-local.postman_environment.json` | Plantilla de variables, sin secretos |
| `fixtures/seed-e2e.sql` | Venues de prueba (la API no puede crearlos) |
| `fixtures/README.md` | Fixtures, operaciones SQL (desactivar usuario, revocar rol) y reinicio de la BD |
| `checklist-e2e-manual.md` | Orden exacto de ejecución, con casillas |
| `registro-ejecucion-manual.md` | Bitácora a rellenar: esperado, observado, PASS/FAIL/BLOCKED |
| `matriz-cobertura-22-rutas.md` | Cobertura de las 22 rutas por rol |

## 2. Requisitos

- JDK 21 y el proyecto PulsePass compilando (`.\mvnw test` en verde).
- PostgreSQL local (o en Docker) y el cliente `psql`.
- Postman Desktop o Web con Desktop Agent.

Dependencias bloqueantes (si falta alguna, los casos afectados son **BLOCKED**, no PASS):
la app arranca; Flyway V1–V4 aplicó; existen las cuentas `admin` y `staff`; `GET /api/auth/me` está implementado.

## 3. Preparar la base de datos E2E

Usa una base **dedicada**. No uses la de desarrollo.

```powershell
psql -U postgres -c "CREATE DATABASE pulsepass_e2e;"
```

## 4. Arrancar la aplicación en modo E2E

Tu `application.yaml` apunta a `pulsepass`; se redirige con variables de entorno, sin tocar el código. En PowerShell, en **la misma ventana** donde arrancarás la app:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/pulsepass_e2e"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<tu-clave-de-postgres>"

# Clave JWT exclusiva de esta sesión E2E (48 bytes aleatorios, Base64)
$b = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$env:PULSEPASS_JWT_SECRET_BASE64 = [Convert]::ToBase64String($b)

# Cuentas privilegiadas LOCALES (solo perfil dev). Mínimo 12 caracteres cada una.
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:PULSEPASS_BOOTSTRAP_ADMIN_PASSWORD = "<frase-admin-12+-caracteres>"
$env:PULSEPASS_BOOTSTRAP_STAFF_PASSWORD = "<frase-staff-12+-caracteres>"

.\mvnw spring-boot:run
```

Qué debe pasar: Flyway aplica V1–V4 y en el log aparece `Bootstrap account 'admin' created` y `... 'staff' created`. Si no aparece, revisa que `SPRING_PROFILES_ACTIVE=dev` esté definida y que las contraseñas tengan 12+ caracteres.

- La clave JWT **no se escribe en ningún archivo**. Si cierras la ventana y reinicias la app con otra clave, los tokens anteriores dejan de valer: vuelve a hacer login.
- Las contraseñas de admin/staff son las que escribas aquí; las necesitarás en Postman.

## 5. Cargar fixtures e importar en Postman

1. Con la app ya arrancada, carga los venues:
   ```powershell
   psql -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
   ```
   Deben salir `VEN-E2E-01` y `VEN-E2E-SMALL`.
2. En Postman: **Import** → arrastra los dos JSON de `postman/`.
3. Arriba a la derecha selecciona el ambiente **PulsePass local**.
4. Abre el ambiente y completa estas variables. Para los secretos escribe en **Current value** (el *Initial value* es el que se comparte o exporta; déjalo vacío):

| Variable | Qué poner |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `runId` | Identificador de la corrida (por defecto `r01`) |
| `userAPassword`, `userBPassword`, `userCPassword`, `userDPassword` | Una frase de 12+ caracteres para cada una (inventada, solo de prueba) |
| `adminPassword`, `staffPassword` | Las mismas que pusiste en `PULSEPASS_BOOTSTRAP_*_PASSWORD` |
| `adminIdentifier`, `staffIdentifier` | `admin` y `staff` (valores por defecto) |
| `artistId` | Vacía: se copia en el caso TC-ART-01 |
| `userAToken` … `foreignToken`, `ticketCancelableCode`, `ticketUsableCode` | Vacías: se copian durante la ejecución |

`expectedIssuer`, `expectedTokenTtlSeconds` y `expectedForeignResourceStatus` son de referencia (no las usa ninguna solicitud): compara a mano contra ellas.

## 6. Cómo ejecutar cada solicitud

Sigue el orden de `checklist-e2e-manual.md`. Para cada caso:

1. Abre la solicitud y lee su descripción (ID, actor, precondiciones, HTTP esperado, qué revisar).
2. Comprueba en la pestaña **Authorization** qué token usa (`{{userAToken}}`, `{{adminToken}}`…).
3. Pulsa **Send**.
4. Revisa **Status**, **Headers** (`Content-Type: application/json`), **Body** y, si el caso muta algo, un GET posterior.
5. Anota en `registro-ejecucion-manual.md`: HTTP observado, qué viste y **PASS / FAIL / BLOCKED**.

**Copiar un token (paso manual, sin scripts):**
1. Envía el login del actor (carpeta 03). Debe ser 200 con `tokenType = Bearer` y `expiresIn = 900`.
2. En el Body de la respuesta copia el valor de `accessToken`, **sin comillas**.
3. Abre el ambiente → pega el valor en el **Current value** de la variable del actor (`userAToken`, `userBToken`, `staffToken`, `adminToken`, `inactiveToken`, `userDToken`) → guarda.
4. Comprueba con `/me` del mismo actor que la identidad es la correcta.

**Copiar un código** (`artistId`, `ticketCancelableCode`, `ticketUsableCode`): igual, desde el JSON de la respuesta, a la variable indicada en la descripción de la solicitud.

**El token dura 15 minutos.** Si empiezas a ver 401 inesperados, repite el login del actor y actualiza su variable. Un 401 por token vencido no es un defecto: anótalo como observación.

**Criterios:**
- **PASS**: HTTP, JSON y condición de negocio/seguridad coinciden con lo esperado.
- **FAIL**: algo no coincide, o hay un 500 no esperado.
- **BLOCKED**: no se puede ejecutar por falta de fixture, actor, token o respuesta previa.
- Un 2xx por sí solo **no** es PASS. Un 403 en una mutación solo es PASS si un GET posterior prueba que el recurso no cambió (los casos ya incluyen esas verificaciones).
- No ejecutes la colección con Runner: cada solicitud se envía individualmente.

## 7. Procedimientos especiales (hacerlos AL FINAL)

Dos casos necesitan reiniciar la app con otra configuración. Como un reinicio puede invalidar tokens, déjalos para el final.

### 7.1 Token expirado (TC-AUTH-010)
1. Detén la app. En la misma ventana de PowerShell (las demás variables siguen definidas): `$env:PULSEPASS_JWT_ACCESS_TTL = "PT20S"` y `.\mvnw spring-boot:run`.
2. Envía el login de USER_A (`TC-AUTH-002`): ahora `expiresIn` será 20. Copia el token a **`expiredToken`**.
3. Espera 25 segundos y envía **TC-AUTH-010**. Esperado: 401.
4. Detén la app y quita la variable: `Remove-Item Env:PULSEPASS_JWT_ACCESS_TTL`.

### 7.2 Token de otro emisor (TC-AUTH-011)
1. `$env:PULSEPASS_JWT_ISSUER = "otro-emisor"` y arranca la app.
2. Login de USER_A → copia el token a **`foreignToken`**.
3. Detén la app, `Remove-Item Env:PULSEPASS_JWT_ISSUER` y arranca de nuevo con la configuración normal (misma clave).
4. Envía **TC-AUTH-011**. Esperado: 401.

Esto no requiere ninguna clave real ni modificar la API: solo se cambia la configuración de la sesión de prueba.

## 8. Repetir la prueba

- **Recomendado:** base limpia (`fixtures/README.md`, sección 4) y mismo `runId`.
- **Sin borrar la base:** cambia `runId` y edita en el ambiente los valores que lo llevan: `userA/B/C/D Email` y `Username`, `publishedEventCode`, `draftEventCode`, `ageEventCode`, `smallEventCode` (p. ej. `r01` → `r02`). Los códigos de evento deben quedar en mayúsculas. Los casos TC-SEC-005 y TC-EVT-20 (búsqueda por artista) pueden mostrar eventos de corridas anteriores: en una base sucia, anótalo como observación.
- En ambos casos reinicia la bitácora y vuelve a copiar los tokens.

## 9. Políticas que la API aplica (lo que debes ver)

| Situación | Esperado |
|---|---|
| Sin token / token inválido, alterado o vencido en ruta protegida | **401** + ErrorResponse |
| Rol insuficiente | **403** + ErrorResponse |
| Ticket o usuario ajeno, como USER | **403** (también si el recurso no existe: anti-enumeración) |
| Ticket inexistente, como STAFF/ADMIN | **404** |
| Evento no publicado, como anónimo o USER | **404** (indistinguible de «no existe») |
| Evento SOLD_OUT, como público | **404** (solo PUBLISHED es público) |
| Login con credenciales malas / usuario inexistente / inactivo | **401**, mismo mensaje «Credenciales inválidas» |
| Bearer inválido en una ruta **pública** | Se ignora: la ruta responde 200 como anónimo |

`ErrorResponse` siempre trae `timestamp`, `status`, `error`, `message`, `details`; `status` coincide con el HTTP y no hay stacktrace, hashes, tokens ni secretos. Un **500** en un escenario nominal hace fallar el recorrido.

Limitación documentada del MVP: **STAFF es global** en `use` y `tickets/paid` (el dominio no asigna personal a eventos). No es un diseño para producción.

## 10. Confidencialidad

- No guardes tokens ni contraseñas en la colección, en el *Initial value* ni en capturas.
- No exportes el ambiente con valores reales; el `.gitignore` de esta carpeta bloquea los exportes locales y las capturas.
- Antes de entregar, revisa que `registro-ejecucion-manual.md` no contenga tokens ni contraseñas.
