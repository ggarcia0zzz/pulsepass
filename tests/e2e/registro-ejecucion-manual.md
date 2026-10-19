# Registro de ejecución manual — PulsePass E2E (Fase 5)

> Bitácora a diligenciar a mano tras cada **Send**. Estados: **PASS** (HTTP, JSON y condición de negocio coinciden), **FAIL** (algo no coincide o hay un 500 no esperado), **BLOCKED** (falta prerrequisito: fixture, actor, token o respuesta previa) y **NOT RUN** (aún no ejecutado).
> No pegues tokens, contraseñas ni datos personales reales en las columnas de evidencia.
> Nota: Todas las evidencias están en la carpeta evidencias

## Datos de la sesión

| Campo | Valor                                                       |
|---|-------------------------------------------------------------|
| Ejecutado por | Gabriela Garcia y Juan David Pardo                          |
| Fecha y hora de inicio | 19-10-2026 / 12:29 a.m.                                     |
| runId | r01                                                         |
| Base de datos | pulsepass_e2e                                               |
| Commit / versión de PulsePass | Feat: capa seguridad con modificación y agregación de tests |
| Política anti-IDOR verificada | Ticket/usuario ajeno → 403; borrador para público → 404     |

## Casos

### 00 Preparación / conectividad

| Caso | Fecha/hora             | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
|---|------------------------|---|---|---|----------------|------------------------|-----------|---|
| TC-PRE-01 | 19-10-2026/ 12:32 a.m. | ANONYMOUS | GET /api/venues/active | 200 | 200            |         | PASS      | pendiente |

### 01 Venues (2 rutas)

| Caso | Fecha/hora             | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
|---|------------------------|---|---|---|----------------|---|-----------|---|
| TC-VEN-01 | 19-10-2026/ 12:55 a.m. | ANONYMOUS | GET /api/venues/{{venueActiveCode}} | 200 | 200            | pendiente | PASS      | pendiente |
| TC-VEN-02 | 19-10-2026/ 1:07 a.m.  | ANONYMOUS | GET /api/venues/active | 200 | 200            | pendiente | PASS      | pendiente |
| TC-VEN-03 | 19-10-2026/ 1:08 a.m.  | ANONYMOUS | GET /api/venues/VEN-NO-EXISTE | 404 | 404              | pendiente | PASS      | pendiente |

### 02 Artists (3 rutas)

| Caso | Fecha/hora            | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
|---|-----------------------|---|---|---|----------------|---|-----------|---|
| TC-ART-01 | 19-10-2026/ 1:13 a.m. | ANONYMOUS | GET /api/artists/active | 200 | 200            | pendiente | PASS      | pendiente |
| TC-ART-02 | 19-10-2026/ 1:20 a.m. | ANONYMOUS | GET /api/artists/{{artistId}} | 200 | 200            | pendiente | PASS      | pendiente |
| TC-ART-03 | 19-10-2026/ 1:29 a.m. | ANONYMOUS | GET /api/artists/by-stage-name?stageName=… | 200 | 200            | pendiente | PASS      | pendiente |
| TC-ART-04 | 19-10-2026/ 1:33 a.m. | ANONYMOUS | GET /api/artists/999999 | 404 | 404            | pendiente | PASS      | pendiente |

### 03 Auth / usuarios (registro, login, me)

| Caso | Fecha/hora | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-AUTH-001 | 19-10-2026/ 1:49 a.m. | ANONYMOUS | POST /api/users | 201 | 201 | pendiente | PASS | pendiente |
| TC-AUTH-001b | 19-10-2026/ 1:50 a.m. | ANONYMOUS | POST /api/users | 201 | 201 | pendiente | PASS | pendiente |
| TC-AUTH-001c | 19-10-2026/ 1:52 a.m. | ANONYMOUS | POST /api/users | 201 | 201 | pendiente | PASS | pendiente |
| TC-AUTH-002 | 19-10-2026/ 1:55 a.m. | ANONYMOUS | POST /api/auth/login | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-002b | 19-10-2026/ 1:56 a.m. | ANONYMOUS | POST /api/auth/login | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-002c | 19-10-2026/ 1:58 a.m. | ANONYMOUS | POST /api/auth/login | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-002d | 19-10-2026/ 2:01 a.m. | ANONYMOUS | POST /api/auth/login | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-002e | 19-10-2026/ 2:02 a.m. | ANONYMOUS | POST /api/auth/login | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-003 | 19-10-2026/ 2:05 a.m. | ANONYMOUS | POST /api/auth/login | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-006 | 19-10-2026/ 2:09 a.m. | USER_A | GET /api/auth/me | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-006b | 19-10-2026/ 2:10 a.m. | USER_B | GET /api/auth/me | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-006c | 19-10-2026/ 2:12 a.m. | STAFF | GET /api/auth/me | 200 | 200 | pendiente | PASS | pendiente |
| TC-AUTH-006d | 19-10-2026/ 2:15 a.m. | ADMIN | GET /api/auth/me | 200 | 200 | pendiente | PASS | pendiente |


### 04 Usuarios / consulta y propiedad (2 rutas)

| Caso | Fecha/hora | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-USR-01 | 19-10-2026/ 2:18 a.m. | USER_A | GET /api/users/by-email?email=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-USR-02 | 19-10-2026/ 2:19 a.m. | USER_A | GET /api/users/by-username?username=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-USR-03 | 19-10-2026/ 2:22 a.m. | ADMIN | GET /api/users/by-email?email=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-USR-04 | 19-10-2026/ 2:23 a.m. | ADMIN | GET /api/users/by-username?username=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-006 | 19-10-2026/ 2:25 a.m. | USER_B | GET /api/users/by-email?email=… | 403 | 403 | pendiente | PASS | pendiente |
| TC-SEC-007 | 19-10-2026/ 2:26 a.m. | USER_B | GET /api/users/by-username?username=… | 403 | 403 | pendiente | PASS | pendiente |
| TC-USR-05 | 19-10-2026/ 2:29 a.m. | ANONYMOUS | GET /api/users/by-email?email=… | 401 | 401 | pendiente | PASS | pendiente |
| TC-USR-06 | 19-10-2026/ 2:30 a.m. | ANONYMOUS | GET /api/users/by-username?username=… | 401 | 401 | pendiente | PASS | pendiente |
| TC-USR-07 | 19-10-2026/ 2:34 a.m. | ADMIN | GET /api/users/by-email?email=… | 404 | 404 | pendiente | PASS | pendiente |
| TC-USR-08 | 19-10-2026/ 2:36 a.m. | USER_A | GET /api/users/by-email?email=… | 403 | 403 | pendiente | PASS | pendiente |


### 05 Events / administración y visibilidad (6 rutas)

| Caso | Fecha/hora | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-EVT-01 | 19-10-2026/ 2:40 a.m. | ADMIN | POST /api/events | 201 | 201 | pendiente | PASS | pendiente |
| TC-EVT-02 | 19-10-2026/ 2:42 a.m. | ADMIN | POST /api/events | 201 | 201 | pendiente | PASS | pendiente |
| TC-SEC-001 | 19-10-2026/ 2:43 a.m. | ANONYMOUS | POST /api/events | 401 | 401 | pendiente | PASS | pendiente |
| TC-SEC-001b | 19-10-2026/ 2:45 a.m. | USER_A | POST /api/events | 403 | 403 | pendiente | PASS | pendiente |
| TC-EVT-03 | 19-10-2026/ 2:48 a.m. | ADMIN | POST /api/events | 409 | 409 | pendiente | PASS | pendiente |
| TC-EVT-04 | 19-10-2026/ 2:50 a.m. | ADMIN | POST /api/events | 404 | 404 | pendiente | PASS | pendiente |
| TC-EVT-05 | 19-10-2026/ 2:51 a.m. | ADMIN | POST /api/events | 409 | 409 | pendiente | PASS | pendiente |
| TC-EVT-06 | 19-10-2026/ 2:54 a.m. | ADMIN | POST /api/events | 400 | 400 | pendiente | PASS | pendiente |
| TC-EVT-07 | 19-10-2026/ 2:55 a.m. | ADMIN | POST /api/events | 400 | 400 | pendiente | PASS | pendiente |
| TC-SEC-004 | 19-10-2026/ 2:58 a.m. | ANONYMOUS | GET /api/events/{{draftEventCode}} | 404 | 404 | pendiente | PASS | pendiente |
| TC-SEC-004b | 19-10-2026/ 2:59 a.m. | USER_A | GET /api/events/{{draftEventCode}} | 404 | 404 | pendiente | PASS | pendiente |
| TC-SEC-015 | 19-10-2026/ 3:02 a.m. | ADMIN | GET /api/events/{{draftEventCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-08 | 19-10-2026/ 3:03 a.m. | STAFF | GET /api/events/{{draftEventCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-09 | 19-10-2026/ 3:06 a.m. | ANONYMOUS | GET /api/events/published | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-003 | 19-10-2026/ 3:08 a.m. | USER_A | POST /api/events/{{publishedEventCode}}/artists/{{artistId}} | 403 | 403 | pendiente | PASS | pendiente |
| TC-EVT-10 | 19-10-2026/ 3:11 a.m. | ADMIN | POST /api/events/{{publishedEventCode}}/artists/{{artistId}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-11 | 19-10-2026/ 3:12 a.m. | ADMIN | POST /api/events/{{publishedEventCode}}/artists/{{artistId}} | 409 | 409 | pendiente | PASS | pendiente |
| TC-EVT-12 | 19-10-2026/ 3:15 a.m. | ADMIN | POST /api/events/{{publishedEventCode}}/artists/999999 | 404 | 404 | pendiente | PASS | pendiente |
| TC-EVT-13 | 19-10-2026/ 3:17 a.m. | ADMIN | POST /api/events/E2E-NO-EXISTE/artists/{{artistId}} | 404 | 404 | pendiente | PASS | pendiente |
| TC-SEC-005 | 19-10-2026/ 3:20 a.m. | ANONYMOUS | GET /api/events/by-artist?stageName=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-14 | 19-10-2026/ 3:21 a.m. | STAFF | GET /api/events/by-artist?stageName=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-002 | 19-10-2026/ 3:24 a.m. | USER_A | PATCH /api/events/{{publishedEventCode}}/publish | 403 | 403 | pendiente | PASS | pendiente |
| TC-SEC-002b | 19-10-2026/ 3:25 a.m. | STAFF | PATCH /api/events/{{publishedEventCode}}/publish | 403 | 403 | pendiente | PASS | pendiente |
| TC-EVT-15 | 19-10-2026/ 3:27 a.m. | ANONYMOUS | PATCH /api/events/{{publishedEventCode}}/publish | 401 | 401 | pendiente | PASS | pendiente |
| TC-EVT-16 | 19-10-2026/ 3:30 a.m. | ADMIN | PATCH /api/events/{{publishedEventCode}}/publish | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-17 | 19-10-2026/ 3:31 a.m. | ADMIN | PATCH /api/events/{{publishedEventCode}}/publish | 409 | 409 | pendiente | PASS | pendiente |
| TC-EVT-17b | 19-10-2026/ 3:33 a.m. | ADMIN | PATCH /api/events/E2E-NO-EXISTE/publish | 404 | 404 | pendiente | PASS | pendiente |
| TC-EVT-18 | 19-10-2026/ 3:36 a.m. | ANONYMOUS | GET /api/events/{{publishedEventCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-19 | 19-10-2026/ 3:37 a.m. | ANONYMOUS | GET /api/events/published | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-20 | 19-10-2026/ 3:39 a.m. | ANONYMOUS | GET /api/events/by-artist?stageName=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-21 | 19-10-2026/ 3:42 a.m. | ANONYMOUS | GET /api/events/E2E-NO-EXISTE | 404 | 404 | pendiente | PASS | pendiente |
| TC-EVT-22 | 19-10-2026/ 3:45 a.m. | ADMIN | POST /api/events | 201 | 201 | pendiente | PASS | pendiente |
| TC-EVT-23 | 19-10-2026/ 3:47 a.m. | ADMIN | PATCH /api/events/{{ageEventCode}}/publish | 200 | 200 | pendiente | PASS | pendiente |
| TC-EVT-24 | 19-10-2026/ 3:50 a.m. | ADMIN | POST /api/events | 201 | 201 | pendiente | PASS | pendiente |
| TC-EVT-25 | 19-10-2026/ 3:52 a.m. | ADMIN | PATCH /api/events/{{smallEventCode}}/publish | 200 | 200 | pendiente | PASS | pendiente |


### 06 Tickets / compra, consulta y ciclo de vida (6 rutas)

| Caso | Fecha/hora | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-TKT-01 | 19-10-2026/ 3:56 a.m. | USER_A | POST /api/tickets | 201 | 201 | pendiente | PASS | pendiente |
| TC-TKT-02 | 19-10-2026/ 3:58 a.m. | USER_A | POST /api/tickets | 201 | 201 | pendiente | PASS | pendiente |
| TC-TKT-03 | 19-10-2026/ 4:01 a.m. | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-04 | 19-10-2026/ 4:02 a.m. | USER_A | GET /api/tickets/by-user?email=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-009 | 19-10-2026/ 4:05 a.m. | USER_B | GET /api/tickets/{{ticketCancelableCode}} | 403 | 403 | pendiente | PASS | pendiente |
| TC-SEC-010 | 19-10-2026/ 4:06 a.m. | USER_B | GET /api/tickets/by-user?email=… | 403 | 403 | pendiente | PASS | pendiente |
| TC-SEC-008 | 19-10-2026/ 4:09 a.m. | USER_A | POST /api/tickets | 403 | 403 | pendiente | PASS | pendiente |
| TC-TKT-05 | 19-10-2026/ 4:11 a.m. | USER_B | GET /api/tickets/by-user?email=… | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-06 | 19-10-2026/ 4:14 a.m. | ANONYMOUS | POST /api/tickets | 401 | 401 | pendiente | PASS | pendiente |
| TC-SEC-011 | 19-10-2026/ 4:16 a.m. | USER_B | PATCH /api/tickets/{{ticketCancelableCode}}/cancel | 403 | 403 | pendiente | PASS | pendiente |
| TC-SEC-012 | 19-10-2026/ 4:18 a.m. | USER_B | PATCH /api/tickets/{{ticketCancelableCode}}/use | 403 | 403 | pendiente | PASS | pendiente |
| TC-SEC-012b | 19-10-2026/ 4:19 a.m. | USER_A | PATCH /api/tickets/{{ticketUsableCode}}/use | 403 | 403 | pendiente | PASS | pendiente |
| TC-TKT-07 | 19-10-2026/ 4:22 a.m. | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-013 | 19-10-2026/ 4:25 a.m. | USER_A | GET /api/events/{{publishedEventCode}}/tickets/paid | 403 | 403 | pendiente | PASS | pendiente |
| TC-TKT-08 | 19-10-2026/ 4:26 a.m. | STAFF | GET /api/events/{{publishedEventCode}}/tickets/paid | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-09 | 19-10-2026/ 4:28 a.m. | ADMIN | GET /api/events/{{publishedEventCode}}/tickets/paid | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-10 | 19-10-2026/ 4:31 a.m. | USER_A | PATCH /api/tickets/{{ticketCancelableCode}}/cancel | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-11 | 19-10-2026/ 4:33 a.m. | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-12 | 19-10-2026/ 4:35 a.m. | USER_A | PATCH /api/tickets/{{ticketCancelableCode}}/cancel | 409 | 409 | pendiente | PASS | pendiente |
| TC-TKT-13 | 19-10-2026/ 4:38 a.m. | STAFF | PATCH /api/tickets/{{ticketCancelableCode}}/use | 409 | 409 | pendiente | PASS | pendiente |
| TC-TKT-14 | 19-10-2026/ 4:40 a.m. | STAFF | PATCH /api/tickets/{{ticketUsableCode}}/use | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-15 | 19-10-2026/ 4:41 a.m. | USER_A | GET /api/tickets/{{ticketUsableCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-16 | 19-10-2026/ 4:44 a.m. | STAFF | PATCH /api/tickets/{{ticketUsableCode}}/use | 409 | 409 | pendiente | PASS | pendiente |
| TC-TKT-17 | 19-10-2026/ 4:46 a.m. | USER_A | PATCH /api/tickets/{{ticketUsableCode}}/cancel | 409 | 409 | pendiente | PASS | pendiente |
| TC-TKT-18 | 19-10-2026/ 4:48 a.m. | STAFF | GET /api/events/{{publishedEventCode}}/tickets/paid | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-19 | 19-10-2026/ 4:51 a.m. | USER_A | POST /api/tickets | 409 | 409 | pendiente | PASS | pendiente |
| TC-TKT-20 | 19-10-2026/ 4:53 a.m. | USER_A | POST /api/tickets | 409 | 409 | pendiente | PASS | pendiente |
| TC-TKT-21 | 19-10-2026/ 4:55 a.m. | USER_A | POST /api/tickets | 404 | 404 | pendiente | PASS | pendiente |
| TC-TKT-22 | 19-10-2026/ 4:57 a.m. | USER_A | POST /api/tickets | 400 | 400 | pendiente | PASS | pendiente |
| TC-TKT-23 | 19-10-2026/ 4:58 a.m. | USER_A | POST /api/tickets | 400 | 400 | pendiente | PASS | pendiente |
| TC-TKT-24 | 19-10-2026/ 5:01 a.m. | USER_A | POST /api/tickets | 201 | 201 | pendiente | PASS | pendiente |
| TC-TKT-25 | 19-10-2026/ 5:03 a.m. | USER_B | POST /api/tickets | 409 | 409 | pendiente | PASS | pendiente |
| TC-TKT-26 | 19-10-2026/ 5:06 a.m. | ADMIN | GET /api/events/{{smallEventCode}} | 200 | 200 | pendiente | PASS | pendiente |
| TC-TKT-27 | 19-10-2026/ 5:08 a.m. | ANONYMOUS | GET /api/events/{{smallEventCode}} | 404 | 404 | pendiente | PASS | pendiente |
| TC-TKT-28 | 19-10-2026/ 5:11 a.m. | STAFF | GET /api/tickets/TCK-NO-EXISTE | 404 | 404 | pendiente | PASS | pendiente |
| TC-TKT-29 | 19-10-2026/ 5:13 a.m. | USER_A | GET /api/tickets/TCK-NO-EXISTE | 403 | 403 | pendiente | PASS | pendiente |
| TC-TKT-30 | 19-10-2026/ 5:15 a.m. | STAFF | GET /api/tickets/{{ticketUsableCode}} | 200 | 200 | pendiente | PASS | pendiente |

### 07 Seguridad / escenarios negativos

| Caso | Fecha/hora | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-AUTH-007 | 19-10-2026/ 5:19 a.m. | ANONYMOUS | GET /api/auth/me | 401 | 401 | pendiente | PASS | pendiente |
| TC-AUTH-008 | 19-10-2026/ 5:20 a.m. | ANONYMOUS | GET /api/auth/me | 401 | 401 | pendiente | PASS | pendiente |
| TC-AUTH-009 | 19-10-2026/ 5:22 a.m. | USER_A (token alterado) | GET /api/auth/me | 401 | 401 | pendiente | PASS | pendiente |
| TC-AUTH-012 | 19-10-2026/ 5:24 a.m. | ANONYMOUS | GET /api/auth/me?access_token=… | 401 | 401 | pendiente | PASS | pendiente |
| TC-AUTH-014 | 19-10-2026/ 5:27 a.m. | USER_A | POST /api/events | 403 | 403 | pendiente | PASS | pendiente |
| TC-AUTH-015 | 19-10-2026/ 5:29 a.m. | ANONYMOUS | GET /api/events/published | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-016 | 19-10-2026/ 5:32 a.m. | ANONYMOUS | POST /api/users | 201 | 201 | pendiente | PASS | pendiente |
| TC-SEC-016b | 19-10-2026/ 5:33 a.m. | ANONYMOUS | POST /api/auth/login | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-016c | 19-10-2026/ 5:35 a.m. | USER_D | GET /api/auth/me | 200 | 200 | pendiente | PASS | pendiente |
| TC-SEC-016d | 19-10-2026/ 5:38 a.m. | USER_D | POST /api/events | 403 | 403 | pendiente | PASS | pendiente |
| TC-AUTH-013 | 19-10-2026/ 5:40 a.m. | INACTIVE_USER | GET /api/auth/me | 401 | 401 | pendiente | PASS | pendiente |
| TC-AUTH-013b | 19-10-2026/ 5:42 a.m. | ANONYMOUS | POST /api/auth/login | 401 | 401 | pendiente | PASS | pendiente |
| TC-SEC-017 | 19-10-2026/ 5:45 a.m. | STAFF | GET /api/events/{{publishedEventCode}}/tickets/paid | 403 | 403 | pendiente | PASS | pendiente |
| TC-AUTH-010 | 19-10-2026/ 5:47 a.m. | USER_A (token expirado) | GET /api/auth/me | 401 | 401 | pendiente | PASS | pendiente |
| TC-AUTH-011 | 19-10-2026/ 5:49 a.m. | USER_A (otro emisor) | GET /api/auth/me | 401 | 401 | pendiente | PASS | pendiente |


### 08 Validaciones y errores HTTP

| Caso | Fecha/hora | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-AUTH-004 | 19-10-2026/ 5:52 a.m. | ANONYMOUS | POST /api/auth/login | 401 | 401 | pendiente | PASS | pendiente |
| TC-AUTH-005 | 19-10-2026/ 5:54 a.m. | ANONYMOUS | POST /api/auth/login | 401 | 401 | pendiente | PASS | pendiente |
| TC-VAL-01 | 19-10-2026/ 5:56 a.m. | ANONYMOUS | POST /api/auth/login | 400 | 400 | pendiente | PASS | pendiente |
| TC-VAL-02 | 19-10-2026/ 5:58 a.m. | ANONYMOUS | POST /api/users | 400 | 400 | pendiente | PASS | pendiente |
| TC-VAL-03 | 19-10-2026/ 6:00 a.m. | ANONYMOUS | POST /api/users | 400 | 400 | pendiente | PASS | pendiente |
| TC-VAL-04 | 19-10-2026/ 6:02 a.m. | ANONYMOUS | POST /api/users | 400 | 400 | pendiente | PASS | pendiente |
| TC-VAL-05 | 19-10-2026/ 6:05 a.m. | ANONYMOUS | POST /api/users | 409 | 409 | pendiente | PASS | pendiente |
| TC-VAL-06 | 19-10-2026/ 6:06 a.m. | ANONYMOUS | POST /api/users | 409 | 409 | pendiente | PASS | pendiente |
| TC-VAL-07 | 19-10-2026/ 6:09 a.m. | ANONYMOUS | POST /api/users | 400 | 400 | pendiente | PASS | pendiente |
| TC-VAL-08 | 19-10-2026/ 6:11 a.m. | ADMIN | GET /api/users/by-email | 400 | 400 | pendiente | PASS | pendiente |
| TC-VAL-09 | 19-10-2026/ 6:14 a.m. | ANONYMOUS | GET /api/ruta-no-declarada | 401 | 401 | pendiente | PASS | pendiente |
| TC-VAL-10 | 19-10-2026/ 6:16 a.m. | ADMIN | GET /api/ruta-no-declarada | 404 | 404 | pendiente | PASS | pendiente |
| TC-VAL-11 | 19-10-2026/ 6:18 a.m. | ADMIN | DELETE /api/events/{{publishedEventCode}} | 405 | 405 | pendiente | PASS | pendiente |


## Consolidación por módulo (completar al terminar)

| Módulo | PASS | FAIL | BLOCKED | NOT RUN |
|---|------|------|---|---|
| 00 Preparación / conectividad | x    |      | | |
| 01 Venues (2 rutas) | x    |      | | |
| 02 Artists (3 rutas) | x    |      | | |
| 03 Auth / usuarios (registro, login, me) | x    | x    | | |
| 04 Usuarios / consulta y propiedad (2 rutas) |      |      | | |
| 05 Events / administración y visibilidad (6 rutas) |      |      | | |
| 06 Tickets / compra, consulta y ciclo de vida (6 rutas) |      |      | | |
| 07 Seguridad / escenarios negativos |      |      | | |
| 08 Validaciones y errores HTTP |      |      | | |
| **Total** |      |      | | |

Las validaciones de seguridad (carpeta 07 y los casos TC-SEC) se cuentan además por separado:

| Seguridad | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|
| TC-SEC / TC-AUTH negativos | | | | |

## Cobertura de las 22 rutas

| # | Ruta | Caso nominal | Resultado nominal | Casos negativos ejecutados |
|---|---|---|---|---|
| 1 | GET /api/venues/{code} | TC-VEN-01 | NOT RUN | __ de 1 |
| 2 | GET /api/venues/active | TC-VEN-02 | NOT RUN | __ de 0 |
| 3 | POST /api/events | TC-EVT-01, TC-EVT-02, TC-EVT-22 … | NOT RUN | __ de 9 |
| 4 | GET /api/events/{eventCode} | TC-SEC-015, TC-EVT-08, TC-EVT-18 … | NOT RUN | __ de 4 |
| 5 | GET /api/events/published | TC-EVT-09, TC-EVT-19, TC-AUTH-015 | NOT RUN | __ de 0 |
| 6 | PATCH /api/events/{eventCode}/publish | TC-EVT-16, TC-EVT-23, TC-EVT-25 | NOT RUN | __ de 5 |
| 7 | POST /api/events/{eventCode}/artists/{artistId} | TC-EVT-10 | NOT RUN | __ de 4 |
| 8 | GET /api/events/by-artist?stageName=... | TC-SEC-005, TC-EVT-14, TC-EVT-20 | NOT RUN | __ de 0 |
| 9 | GET /api/artists/{id} | TC-ART-02 | NOT RUN | __ de 1 |
| 10 | GET /api/artists/by-stage-name?stageName=... | TC-ART-03 | NOT RUN | __ de 0 |
| 11 | GET /api/artists/active | TC-ART-01 | NOT RUN | __ de 0 |
| 12 | POST /api/users | TC-AUTH-001, TC-AUTH-001b, TC-AUTH-001c | NOT RUN | __ de 7 |
| 13 | GET /api/users/by-email?email=... | TC-USR-01, TC-USR-03 | NOT RUN | __ de 5 |
| 14 | GET /api/users/by-username?username=... | TC-USR-02, TC-USR-04 | NOT RUN | __ de 2 |
| 15 | POST /api/tickets | TC-TKT-01, TC-TKT-02, TC-TKT-24 | NOT RUN | __ de 8 |
| 16 | GET /api/tickets/{ticketCode} | TC-TKT-03, TC-TKT-07, TC-TKT-11 … | NOT RUN | __ de 3 |
| 17 | GET /api/tickets/by-user?email=... | TC-TKT-04, TC-TKT-05 | NOT RUN | __ de 1 |
| 18 | GET /api/events/{eventCode}/tickets/paid | TC-TKT-08, TC-TKT-09, TC-TKT-18 | NOT RUN | __ de 2 |
| 19 | PATCH /api/tickets/{ticketCode}/cancel | TC-TKT-10 | NOT RUN | __ de 3 |
| 20 | PATCH /api/tickets/{ticketCode}/use | TC-TKT-14 | NOT RUN | __ de 4 |
| 21 | POST /api/auth/login | TC-AUTH-002, TC-AUTH-002b, TC-AUTH-002c … | NOT RUN | __ de 4 |
| 22 | GET /api/auth/me | TC-AUTH-006, TC-AUTH-006b, TC-AUTH-006c … | NOT RUN | __ de 7 |

## Defectos y bloqueos encontrados

| ID | Caso | Precondiciones | Pasos para reproducir | HTTP esperado | HTTP observado | Severidad | Estado |
|---|---|---|---|---|---|---|---|
| DEF-01 | | | | | | | |

## Regresión respecto a las Fases 1–4 (FR-E2E-014)

Anota aquí cualquier incompatibilidad detectada con los contratos anteriores:

- 
