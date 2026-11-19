# Ejecución local
Requiere Java 21 y PostgreSQL. Crear la base pulsepass si no existe. En PowerShell, desde la raíz:

```powershell
$env:DB_USERNAME='postgres'
$env:DB_PASSWORD='TU_PASSWORD_POSTGRES'
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:PULSEPASS_JWT_SECRET_BASE64=[Convert]::ToBase64String($bytes)
.\mvnw.cmd spring-boot:run
```

Registrar tres cuentas mediante la pantalla Registro del frontend, usando correos admin@pulsepass.test, staff@pulsepass.test y user@pulsepass.test y contraseñas propias de al menos 12 caracteres. Ejecutar dev/roles.sql en pgAdmin sobre pulsepass. Iniciar sesión de nuevo para aplicar permisos. No usar estas instrucciones sobre producción. Flyway crea tablas, artistas y sedes. Crear y publicar un evento desde ADMIN para probar compras. No se modificaron reglas de negocio ni controladores.
