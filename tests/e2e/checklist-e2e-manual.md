# Checklist de ejecución manual — PulsePass E2E (Fase 5)

Marca cada casilla **solo después** de enviar la solicitud, inspeccionar Status, Body y (si aplica) un GET posterior, y anotar el resultado en `registro-ejecucion-manual.md`.

> Un HTTP 2xx por sí solo no aprueba un caso. Si un prerrequisito falla, marca los dependientes como **BLOCKED**, no como PASS.

## Fase 0 — Preparación (ver README, secciones 2 a 5)

- [x] Base `pulsepass_e2e` creada y aplicación arrancada con las variables de entorno (Flyway aplicó V1–V4)
- [x] Cuentas `admin` y `staff` creadas por la semilla dev (`SPRING_PROFILES_ACTIVE=dev` + contraseñas por entorno)
- [x] `fixtures/seed-e2e.sql` ejecutado (venues VEN-E2E-01 y VEN-E2E-SMALL)
- [x] Colección y ambiente importados; ambiente «PulsePass local» seleccionado
- [x] Variables completadas: `baseUrl`, `runId`, contraseñas (`userA/B/C/DPassword`, `adminPassword`, `staffPassword`)

## Fase 1 — Conectividad y catálogo público (Flujo A, parte pública)

### Carpeta 00 Preparación / conectividad

- [x] **TC-PRE-01** — Conectividad: la API responde (usa una ruta pública existente) → **HTTP 200** (ANONYMOUS)

### Carpeta 01 Venues (2 rutas)

- [x] **TC-VEN-01** — Consultar venue activo por código → **HTTP 200** (ANONYMOUS)
- [x] **TC-VEN-02** — Listar venues activos → **HTTP 200** (ANONYMOUS)
- [x] **TC-VEN-03** — Venue inexistente → **HTTP 404** (ANONYMOUS)

### Carpeta 02 Artists (3 rutas)

- [x] **TC-ART-01** — Listar artistas activos (y elegir artistId) → **HTTP 200** (ANONYMOUS)  
  ↳ **Copiar:** Del objeto «Solar Beat» copia su `id` a la variable **artistId**.
- [x] **TC-ART-02** — Consultar artista por ID → **HTTP 200** (ANONYMOUS)
- [x] **TC-ART-03** — Consultar artista por nombre artístico → **HTTP 200** (ANONYMOUS)
- [x] **TC-ART-04** — Artista inexistente → **HTTP 404** (ANONYMOUS)

## Fase 2 — Registro, login y /me (Flujo B)

### Carpeta 03 Auth / usuarios (registro, login, me)

- [x] **TC-AUTH-001** — Registrar USER_A → **HTTP 201** (ANONYMOUS)
- [x] **TC-AUTH-001b** — Registrar USER_B → **HTTP 201** (ANONYMOUS)
- [x] **TC-AUTH-001c** — Registrar USER_C (será el usuario inactivo) → **HTTP 201** (ANONYMOUS)
- [x] **TC-AUTH-002** — Login de USER_A con username → **HTTP 200** (ANONYMOUS)  
  ↳ **Copiar:** Copia SOLO el valor de `accessToken` a la variable **userAToken**.
- [x] **TC-AUTH-002b** — Login de USER_B → **HTTP 200** (ANONYMOUS)  
  ↳ **Copiar:** Copia `accessToken` a **userBToken**.
- [x] **TC-AUTH-002c** — Login de STAFF (fixture privilegiado) → **HTTP 200** (ANONYMOUS)  
  ↳ **Copiar:** Copia `accessToken` a **staffToken**.
- [x] **TC-AUTH-002d** — Login de ADMIN (fixture privilegiado) → **HTTP 200** (ANONYMOUS)  
  ↳ **Copiar:** Copia `accessToken` a **adminToken**.
- [x] **TC-AUTH-002e** — Login de USER_C (futuro inactivo) → **HTTP 200** (ANONYMOUS)  
  ↳ **Copiar:** Copia `accessToken` a **inactiveToken**.
- [x] **TC-AUTH-003** — Login de USER_A con email → **HTTP 200** (ANONYMOUS)
- [x] **TC-AUTH-006** — /me con token de USER_A → **HTTP 200** (USER_A)
- [x] **TC-AUTH-006b** — /me con token de USER_B → **HTTP 200** (USER_B)
- [x] **TC-AUTH-006c** — /me con token de STAFF → **HTTP 200** (STAFF)
- [x] **TC-AUTH-006d** — /me con token de ADMIN → **HTTP 200** (ADMIN)

## Fase 3 — Consulta de usuarios y propiedad

### Carpeta 04 Usuarios / consulta y propiedad (2 rutas)

- [x] **TC-USR-01** — USER_A consulta su propio usuario por email → **HTTP 200** (USER_A)
- [x] **TC-USR-02** — USER_A consulta su propio usuario por username → **HTTP 200** (USER_A)
- [x] **TC-USR-03** — ADMIN consulta a USER_A por email → **HTTP 200** (ADMIN)
- [x] **TC-USR-04** — ADMIN consulta a USER_A por username → **HTTP 200** (ADMIN)
- [x] **TC-SEC-006** — USER_B consulta a USER_A por email → denegado → **HTTP 403** (USER_B)
- [x] **TC-SEC-007** — USER_B consulta a USER_A por username → denegado → **HTTP 403** (USER_B)
- [x] **TC-USR-05** — Anónimo consulta usuario por email → 401 → **HTTP 401** (ANONYMOUS)
- [x] **TC-USR-06** — Anónimo consulta usuario por username → 401 → **HTTP 401** (ANONYMOUS)
- [x] **TC-USR-07** — ADMIN consulta un email que no existe → 404 → **HTTP 404** (ADMIN)
- [x] **TC-USR-08** — USER_A consulta un email inexistente → 403 (anti-enumeración) → **HTTP 403** (USER_A)

## Fase 4 — Catálogo de eventos: crear, asociar, publicar y visibilidad (Flujo C)

### Carpeta 05 Events / administración y visibilidad (6 rutas)

- [x] **TC-EVT-01** — ADMIN crea el evento a publicar (publishedEventCode) → **HTTP 201** (ADMIN)
- [x] **TC-EVT-02** — ADMIN crea un evento que se quedará en borrador (draftEventCode) → **HTTP 201** (ADMIN)
- [x] **TC-SEC-001** — Anónimo intenta crear evento → 401 → **HTTP 401** (ANONYMOUS)
- [x] **TC-SEC-001b** — USER_A intenta crear evento → 403 → **HTTP 403** (USER_A)
- [x] **TC-EVT-03** — Crear evento con código duplicado → 409 → **HTTP 409** (ADMIN)
- [x] **TC-EVT-04** — Crear evento con venue inexistente → 404 → **HTTP 404** (ADMIN)
- [x] **TC-EVT-05** — Crear evento con fecha pasada → 409 → **HTTP 409** (ADMIN)
- [x] **TC-EVT-06** — Crear evento con cuerpo vacío → 400 → **HTTP 400** (ADMIN)
- [x] **TC-EVT-07** — Crear evento con categoría inválida → 400 → **HTTP 400** (ADMIN)
- [x] **TC-SEC-004** — Anónimo consulta el borrador por código → 404 sin filtración → **HTTP 404** (ANONYMOUS)
- [x] **TC-SEC-004b** — USER_A consulta el borrador por código → 404 → **HTTP 404** (USER_A)
- [x] **TC-SEC-015** — ADMIN consulta el borrador → 200 → **HTTP 200** (ADMIN)
- [x] **TC-EVT-08** — STAFF consulta el borrador → 200 → **HTTP 200** (STAFF)
- [x] **TC-EVT-09** — Listado público: no incluye borradores ni el evento aún sin publicar → **HTTP 200** (ANONYMOUS)
- [x] **TC-SEC-003** — USER_A intenta asociar artista → 403 → **HTTP 403** (USER_A)
- [x] **TC-EVT-10** — ADMIN asocia el artista al evento → **HTTP 200** (ADMIN)
- [x] **TC-EVT-11** — Asociar el mismo artista otra vez → 409 → **HTTP 409** (ADMIN)
- [x] **TC-EVT-12** — Asociar un artista inexistente → 404 → **HTTP 404** (ADMIN)
- [x] **TC-EVT-13** — Asociar artista a un evento inexistente → 404 → **HTTP 404** (ADMIN)
- [x] **TC-SEC-005** — Anónimo busca eventos por artista (con el evento aún en DRAFT) → lista vacía → **HTTP 200** (ANONYMOUS)
- [x] **TC-EVT-14** — STAFF busca eventos por artista → ve el borrador → **HTTP 200** (STAFF)
- [x] **TC-SEC-002** — USER_A intenta publicar → 403 → **HTTP 403** (USER_A)
- [x] **TC-SEC-002b** — STAFF intenta publicar → 403 → **HTTP 403** (STAFF)
- [x] **TC-EVT-15** — Anónimo intenta publicar → 401 → **HTTP 401** (ANONYMOUS)
- [x] **TC-EVT-16** — ADMIN publica el evento → **HTTP 200** (ADMIN)
- [x] **TC-EVT-17** — Publicar de nuevo un evento ya publicado → 409 → **HTTP 409** (ADMIN)
- [x] **TC-EVT-17b** — Publicar un evento inexistente → 404 → **HTTP 404** (ADMIN)
- [x] **TC-EVT-18** — Anónimo consulta el evento ya publicado → 200 → **HTTP 200** (ANONYMOUS)
- [x] **TC-EVT-19** — Listado público: ahora incluye el evento publicado y NO el borrador → **HTTP 200** (ANONYMOUS)
- [x] **TC-EVT-20** — Anónimo busca eventos por artista → solo publicados → **HTTP 200** (ANONYMOUS)
- [x] **TC-EVT-21** — Evento inexistente → 404 → **HTTP 404** (ANONYMOUS)
- [x] **TC-EVT-22** — ADMIN crea el evento de edad mínima 99 (ageEventCode) → **HTTP 201** (ADMIN)
- [x] **TC-EVT-23** — ADMIN publica el evento de edad mínima 99 → **HTTP 200** (ADMIN)
- [x] **TC-EVT-24** — ADMIN crea el evento de aforo 1 en VEN-E2E-SMALL (smallEventCode) → **HTTP 201** (ADMIN)
- [x] **TC-EVT-25** — ADMIN publica el evento de aforo 1 → **HTTP 200** (ADMIN)

## Fase 5 — Tickets: compra, propiedad, cancelación y uso (Flujo D)

### Carpeta 06 Tickets / compra, consulta y ciclo de vida (6 rutas)

- [x] **TC-TKT-01** — USER_A compra el ticket T1 (será el cancelable) → **HTTP 201** (USER_A)  
  ↳ **Copiar:** Copia `ticketCode` a **ticketCancelableCode**.
- [x] **TC-TKT-02** — USER_A compra el ticket T2 (será el usable) → **HTTP 201** (USER_A)  
  ↳ **Copiar:** Copia `ticketCode` a **ticketUsableCode**.
- [x] **TC-TKT-03** — USER_A consulta su ticket T1 → **HTTP 200** (USER_A)
- [x] **TC-TKT-04** — USER_A lista sus tickets → **HTTP 200** (USER_A)
- [x] **TC-SEC-009** — USER_B consulta el ticket de USER_A → 403 → **HTTP 403** (USER_B)
- [x] **TC-SEC-010** — USER_B lista los tickets del email de A → 403 → **HTTP 403** (USER_B)
- [x] **TC-SEC-008** — USER_A intenta comprar con el email de USER_B → 403 → **HTTP 403** (USER_A)
- [x] **TC-TKT-05** — Verificación: USER_B sigue sin tickets (tras la compra ajena denegada) → **HTTP 200** (USER_B)
- [x] **TC-TKT-06** — Anónimo intenta comprar → 401 → **HTTP 401** (ANONYMOUS)
- [x] **TC-SEC-011** — USER_B intenta cancelar el ticket de A → 403 → **HTTP 403** (USER_B)
- [x] **TC-SEC-012** — USER_B intenta marcar como usado el ticket de A → 403 → **HTTP 403** (USER_B)
- [x] **TC-SEC-012b** — USER_A intenta marcar su ticket T2 como usado → 403 → **HTTP 403** (USER_A)
- [x] **TC-TKT-07** — Verificación: T1 sigue PAID tras los intentos denegados de B → **HTTP 200** (USER_A)
- [x] **TC-SEC-013** — USER_A intenta ver los tickets pagados del evento → 403 → **HTTP 403** (USER_A)
- [x] **TC-TKT-08** — STAFF lista los tickets pagados del evento → **HTTP 200** (STAFF)
- [x] **TC-TKT-09** — ADMIN lista los tickets pagados del evento → **HTTP 200** (ADMIN)
- [x] **TC-TKT-10** — USER_A cancela su ticket T1 → **HTTP 200** (USER_A)
- [x] **TC-TKT-11** — Verificación: T1 quedó CANCELLED → **HTTP 200** (USER_A)
- [x] **TC-TKT-12** — Cancelar T1 otra vez → 409 → **HTTP 409** (USER_A)
- [x] **TC-TKT-13** — STAFF intenta usar el ticket cancelado T1 → 409 → **HTTP 409** (STAFF)
- [x] **TC-TKT-14** — STAFF marca T2 como usado → **HTTP 200** (STAFF)
- [x] **TC-TKT-15** — Verificación: T2 quedó USED → **HTTP 200** (USER_A)
- [x] **TC-TKT-16** — STAFF marca T2 como usado por segunda vez → 409 → **HTTP 409** (STAFF)
- [x] **TC-TKT-17** — USER_A intenta cancelar un ticket ya USADO → 409 → **HTTP 409** (USER_A)
- [x] **TC-TKT-18** — STAFF: el listado de pagados ya NO incluye T1 ni T2 → **HTTP 200** (STAFF)
- [x] **TC-TKT-19** — Compra para un evento en DRAFT → 409 → **HTTP 409** (USER_A)
- [x] **TC-TKT-20** — Compra con edad insuficiente (evento minimumAge = 99) → 409 → **HTTP 409** (USER_A)
- [x] **TC-TKT-21** — Compra para un evento inexistente → 404 → **HTTP 404** (USER_A)
- [x] **TC-TKT-22** — Compra con tipo de ticket inválido → 400 → **HTTP 400** (USER_A)
- [x] **TC-TKT-23** — Compra con email mal formado → 400 → **HTTP 400** (USER_A)
- [x] **TC-TKT-24** — Aforo: USER_A compra la única entrada del evento de aforo 1 → **HTTP 201** (USER_A)
- [x] **TC-TKT-25** — Aforo agotado: USER_B intenta comprar → 409 → **HTTP 409** (USER_B)
- [x] **TC-TKT-26** — ADMIN consulta el evento de aforo 1 → SOLD_OUT → **HTTP 200** (ADMIN)
- [x] **TC-TKT-27** — Anónimo consulta el evento SOLD_OUT → 404 (decisión Fase 4) → **HTTP 404** (ANONYMOUS)
- [x] **TC-TKT-28** — STAFF consulta un ticket inexistente → 404 → **HTTP 404** (STAFF)
- [x] **TC-TKT-29** — USER_A consulta un ticket inexistente → 403 (anti-enumeración) → **HTTP 403** (USER_A)
- [x] **TC-TKT-30** — STAFF consulta cualquier ticket (T2) → 200 → **HTTP 200** (STAFF)

## Fase 6 — Validaciones y errores HTTP (Flujo E)

### Carpeta 08 Validaciones y errores HTTP

- [x] **TC-AUTH-004** — Login con contraseña incorrecta → 401 genérico → **HTTP 401** (ANONYMOUS)
- [x] **TC-AUTH-005** — Login con usuario inexistente → 401 idéntico al anterior → **HTTP 401** (ANONYMOUS)
- [x] **TC-VAL-01** — Login con cuerpo vacío → 400 → **HTTP 400** (ANONYMOUS)
- [x] **TC-VAL-02** — Registro con campos obligatorios ausentes → 400 → **HTTP 400** (ANONYMOUS)
- [x] **TC-VAL-03** — Registro con email mal formado → 400 → **HTTP 400** (ANONYMOUS)
- [x] **TC-VAL-04** — Registro con contraseña de menos de 12 caracteres → 400 → **HTTP 400** (ANONYMOUS)
- [x] **TC-VAL-05** — Registro con username duplicado → 409 → **HTTP 409** (ANONYMOUS)
- [x] **TC-VAL-06** — Registro con email duplicado → 409 → **HTTP 409** (ANONYMOUS)
- [x] **TC-VAL-07** — JSON inválido → 400 → **HTTP 400** (ANONYMOUS)
- [x] **TC-VAL-08** — Falta el parámetro obligatorio email → 400 → **HTTP 400** (ADMIN)
- [x] **TC-VAL-09** — Ruta inexistente sin token → 401 (denegar por defecto) → **HTTP 401** (ANONYMOUS)
- [x] **TC-VAL-10** — Ruta inexistente con ADMIN → 404 → **HTTP 404** (ADMIN)
- [x] **TC-VAL-11** — Método no soportado (DELETE /api/events/{code}) → 405 → **HTTP 405** (ADMIN)

## Fase 7 — Seguridad negativa (carpeta 07)

### 7.1 Casos directos (no requieren pasos externos)

- [x] **TC-AUTH-007** — Ruta protegida sin header Authorization → 401 → **HTTP 401** (ANONYMOUS)
- [x] **TC-AUTH-008** — Bearer inventado / malformado → 401 → **HTTP 401** (ANONYMOUS)  
  ↳ **Antes:** En Authorization usa literalmente: Bearer esto-no-es-un-jwt
- [x] **TC-AUTH-009** — JWT con un carácter alterado → 401 → **HTTP 401** (USER_A (token alterado))
- [x] **TC-AUTH-012** — Token enviado por query string en vez de Bearer → 401 → **HTTP 401** (ANONYMOUS)
- [x] **TC-AUTH-014** — Token de USER en endpoint de ADMIN → 403 → **HTTP 403** (USER_A)
- [x] **TC-AUTH-015** — Bearer inválido en ruta pública → se ignora (200) → **HTTP 200** (ANONYMOUS)  
  ↳ **Antes:** En Authorization usa literalmente: Bearer basura
- [x] **TC-SEC-016** — Registro con role=ADMIN inyectado: se ignora (201) → **HTTP 201** (ANONYMOUS)
- [x] **TC-SEC-016b** — Login del usuario D (el que intentó inyectar ADMIN) → **HTTP 200** (ANONYMOUS)  
  ↳ **Copiar:** Copia `accessToken` a **userDToken**.
- [x] **TC-SEC-016c** — /me del usuario D: roles = solo USER → **HTTP 200** (USER_D)
- [x] **TC-SEC-016d** — El usuario D no puede crear eventos → 403 → **HTTP 403** (USER_D)

### 7.2 Usuario inactivo (requiere SQL)

- [x] **Paso manual:** desactivar a USER_C por SQL (`fixtures/README.md` → «Desactivar usuario»). `inactiveToken` debe haberse copiado ANTES.

- [x] **TC-AUTH-013** — Usuario inactivo con token previo → 401 → **HTTP 401** (INACTIVE_USER)  
  ↳ **Antes:** PASO MANUAL ANTES: desactiva a USER_C por SQL (fixtures/README.md, «Desactivar usuario»). inactiveToken se obtuvo antes de desactivar.
- [x] **TC-AUTH-013b** — Login del usuario inactivo → 401 genérico → **HTTP 401** (ANONYMOUS)

### 7.3 Revocación de rol (requiere SQL)

- [x] **Paso manual:** borrar el rol STAFF del usuario staff por SQL (`fixtures/README.md` → «Revocar rol»), conservando el `staffToken` anterior.

- [x] **TC-SEC-017** — Revocar rol STAFF: el token viejo pierde privilegio → 403 → **HTTP 403** (STAFF)  
  ↳ **Antes:** PASO MANUAL ANTES: borra el rol STAFF del usuario staff por SQL (fixtures/README.md, «Revocar rol»). Usa el staffToken que ya tenías. Después RESTAURA el rol.
- [x] **Paso manual:** restaurar el rol STAFF (`fixtures/README.md` → «Restaurar rol»).

### 7.4 Token expirado y otro emisor (requieren reiniciar la app — hacerlo AL FINAL)

- [x] Seguir README sección 7 («Procedimientos especiales»)

- [x] **TC-AUTH-010** — JWT expirado → 401 → **HTTP 401** (USER_A (token expirado))  
  ↳ **Antes:** PROCEDIMIENTO ESPECIAL «Token expirado» (README, sección 7): reinicia la app con PULSEPASS_JWT_ACCESS_TTL=PT20S, haz login, copia el token a **expiredToken** y espera 25 s antes de enviar.
- [x] **TC-AUTH-011** — JWT emitido por otro issuer → 401 → **HTTP 401** (USER_A (otro emisor))  
  ↳ **Antes:** PROCEDIMIENTO ESPECIAL «Otro emisor» (README, sección 7): reinicia con PULSEPASS_JWT_ISSUER=otro-emisor, haz login, copia el token a **foreignToken**, vuelve a reiniciar con el issuer normal y envía.
- [x] **Paso manual:** volver a arrancar la app con la configuración normal (TTL 900 s, issuer pulsepass-api) y repetir login de los actores que sigas usando.

## Fase 8 — Cierre

- [x] Cada una de las 22 rutas tiene al menos un caso nominal ejecutado (ver `matriz-cobertura-22-rutas.md`)
- [x] Los casos omitidos o dependientes de un fallo están marcados BLOCKED / NOT RUN, nunca PASS
- [x] Consolidación por módulo completada en `registro-ejecucion-manual.md`
- [x] Defectos registrados con precondiciones, pasos, HTTP esperado/observado
- [x] Variables y capturas **sin** tokens ni contraseñas antes de entregar o exportar
