# Fixtures E2E

1. Crear la base **vacía** `pulsepass_e2e` y arrancar la app contra ella (Flyway aplica V1 a V5):
   ```
   export DB_URL=jdbc:postgresql://localhost:5432/pulsepass_e2e
   export PULSEPASS_JWT_SECRET_BASE64=$(openssl rand -base64 48)   # solo para esta sesión
   ./mvnw spring-boot:run
   ```
2. Ejecutar `seed-e2e.sql` pasando las contraseñas por variable (ver cabecera del archivo). El script se niega a correr si la base no se llama `pulsepass_e2e`.
3. Para repetir desde cero: `DROP DATABASE pulsepass_e2e; CREATE DATABASE pulsepass_e2e;` y repetir 1 y 2 (o usar otro `runId` en los datos que crees por HTTP).

Fixtures que crea: `e2e_admin` (ADMIN), `e2e_staff` (STAFF), `e2e_inactive` (inactivo), eventos `E2E-DRAFT`, `E2E-PUBLISHED` (con artista Solar Beat), `E2E-TOPUBLISH` y `E2E-ADULTS` (edad mínima 99, para probar rechazo por edad). Sedes y artistas vienen de las migraciones.
