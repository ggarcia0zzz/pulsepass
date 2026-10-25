# Matriz de cobertura — 22 rutas × roles

Cada celda lista el **HTTP esperado** (y entre paréntesis el caso). Generada desde los mismos casos que la colección.

| Caso base | Ruta | ANON | USER_A / USER_D | USER_B | STAFF | ADMIN | JWT inválido / inactivo | Nominal | Negativos |
|---|---|---|---|---|---|---|---|---|---|
| E2E-VEN-01 | `GET /api/venues/{code}` | 200 (TC-VEN-01)<br>404 (TC-VEN-03) | — | — | — | — | — | 1 | 1 |
| E2E-VEN-02 | `GET /api/venues/active` | 200 (TC-VEN-02) | — | — | — | — | — | 1 | 0 |
| E2E-EVT-01 | `POST /api/events` | 401 (TC-SEC-001) | 403 (TC-SEC-001b)<br>403 (TC-AUTH-014)<br>403 (TC-SEC-016d) | — | — | 201 (TC-EVT-01)<br>201 (TC-EVT-02)<br>409 (TC-EVT-03)<br>404 (TC-EVT-04)<br>+5 más | — | 4 | 9 |
| E2E-EVT-02 | `GET /api/events/{eventCode}` | 404 (TC-SEC-004)<br>200 (TC-EVT-18)<br>404 (TC-EVT-21)<br>404 (TC-TKT-27) | 404 (TC-SEC-004b) | — | 200 (TC-EVT-08) | 200 (TC-SEC-015)<br>200 (TC-TKT-26) | — | 4 | 4 |
| E2E-EVT-03 | `GET /api/events/published` | 200 (TC-EVT-09)<br>200 (TC-EVT-19)<br>200 (TC-AUTH-015) | — | — | — | — | — | 3 | 0 |
| E2E-EVT-04 | `PATCH /api/events/{eventCode}/publish` | 401 (TC-EVT-15) | 403 (TC-SEC-002) | — | 403 (TC-SEC-002b) | 200 (TC-EVT-16)<br>409 (TC-EVT-17)<br>404 (TC-EVT-17b)<br>200 (TC-EVT-23)<br>+1 más | — | 3 | 5 |
| E2E-EVT-05 | `POST /api/events/{eventCode}/artists/{artistId}` | — | 403 (TC-SEC-003) | — | — | 200 (TC-EVT-10)<br>409 (TC-EVT-11)<br>404 (TC-EVT-12)<br>404 (TC-EVT-13) | — | 1 | 4 |
| E2E-EVT-06 | `GET /api/events/by-artist?stageName=...` | 200 (TC-SEC-005)<br>200 (TC-EVT-20) | — | — | 200 (TC-EVT-14) | — | — | 3 | 0 |
| E2E-ART-01 | `GET /api/artists/{id}` | 200 (TC-ART-02)<br>404 (TC-ART-04) | — | — | — | — | — | 1 | 1 |
| E2E-ART-02 | `GET /api/artists/by-stage-name?stageName=...` | 200 (TC-ART-03) | — | — | — | — | — | 1 | 0 |
| E2E-ART-03 | `GET /api/artists/active` | 200 (TC-ART-01) | — | — | — | — | — | 1 | 0 |
| E2E-USR-01 | `POST /api/users` | 201 (TC-AUTH-001)<br>201 (TC-AUTH-001b)<br>201 (TC-AUTH-001c)<br>201 (TC-SEC-016)<br>+6 más | — | — | — | — | — | 3 | 7 |
| E2E-USR-02 | `GET /api/users/by-email?email=...` | 401 (TC-USR-05) | 200 (TC-USR-01)<br>403 (TC-USR-08) | 403 (TC-SEC-006) | — | 200 (TC-USR-03)<br>404 (TC-USR-07)<br>400 (TC-VAL-08) | — | 2 | 5 |
| E2E-USR-03 | `GET /api/users/by-username?username=...` | 401 (TC-USR-06) | 200 (TC-USR-02) | 403 (TC-SEC-007) | — | 200 (TC-USR-04) | — | 2 | 2 |
| E2E-TKT-01 | `POST /api/tickets` | 401 (TC-TKT-06) | 201 (TC-TKT-01)<br>201 (TC-TKT-02)<br>403 (TC-SEC-008)<br>409 (TC-TKT-19)<br>+5 más | 409 (TC-TKT-25) | — | — | — | 3 | 8 |
| E2E-TKT-02 | `GET /api/tickets/{ticketCode}` | — | 200 (TC-TKT-03)<br>200 (TC-TKT-07)<br>200 (TC-TKT-11)<br>200 (TC-TKT-15)<br>+1 más | 403 (TC-SEC-009) | 404 (TC-TKT-28)<br>200 (TC-TKT-30) | — | — | 5 | 3 |
| E2E-TKT-03 | `GET /api/tickets/by-user?email=...` | — | 200 (TC-TKT-04) | 403 (TC-SEC-010)<br>200 (TC-TKT-05) | — | — | — | 2 | 1 |
| E2E-TKT-04 | `GET /api/events/{eventCode}/tickets/paid` | — | 403 (TC-SEC-013) | — | 200 (TC-TKT-08)<br>200 (TC-TKT-18)<br>403 (TC-SEC-017) | 200 (TC-TKT-09) | — | 3 | 2 |
| E2E-TKT-05 | `PATCH /api/tickets/{ticketCode}/cancel` | — | 200 (TC-TKT-10)<br>409 (TC-TKT-12)<br>409 (TC-TKT-17) | 403 (TC-SEC-011) | — | — | — | 1 | 3 |
| E2E-TKT-06 | `PATCH /api/tickets/{ticketCode}/use` | — | 403 (TC-SEC-012b) | 403 (TC-SEC-012) | 409 (TC-TKT-13)<br>200 (TC-TKT-14)<br>409 (TC-TKT-16) | — | — | 1 | 4 |
| E2E-AUTH-01 | `POST /api/auth/login` | 200 (TC-AUTH-002)<br>200 (TC-AUTH-002b)<br>200 (TC-AUTH-002c)<br>200 (TC-AUTH-002d)<br>+7 más | — | — | — | — | — | 7 | 4 |
| E2E-AUTH-02 | `GET /api/auth/me` | 401 (TC-AUTH-007)<br>401 (TC-AUTH-008)<br>401 (TC-AUTH-012) | 200 (TC-AUTH-006)<br>200 (TC-SEC-016c) | 200 (TC-AUTH-006b) | 200 (TC-AUTH-006c) | 200 (TC-AUTH-006d) | 401 (TC-AUTH-009)<br>401 (TC-AUTH-013)<br>401 (TC-AUTH-010)<br>401 (TC-AUTH-011) | 5 | 7 |

**Total:** 22 rutas (20 de negocio + login + /me), 131 solicitudes en la colección.

## Políticas implementadas que la matriz da por supuestas

| Situación | Respuesta |
|---|---|
| Sin token en ruta protegida / token inválido, vencido o alterado | 401 + ErrorResponse |
| Rol insuficiente | 403 + ErrorResponse |
| Ticket o usuario ajeno (actor USER) | 403 (también si el ticket/usuario no existe: anti-enumeración) |
| Ticket inexistente consultado por STAFF/ADMIN | 404 |
| Evento no publicado consultado por anónimo o USER | 404 (idéntico a «no existe») |
| Evento SOLD_OUT consultado por el público | 404 (solo PUBLISHED es público; decisión de Fase 4) |
| Login incorrecto / usuario inexistente / usuario inactivo | 401 «Credenciales inválidas» (mismo cuerpo) |
| STAFF | Alcance global en `use` y `tickets/paid` (limitación documentada del MVP) |
