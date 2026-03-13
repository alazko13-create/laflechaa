# UnaFlecha Native Android

Proyecto Android base para envolver `https://unaflecha.com` y sumar funciones nativas para choferes:

- WebView cargando la web real
- servicio foreground para ubicación en segundo plano
- polling nativo usando endpoints existentes del ZIP:
  - `/api/token.php`
  - `/api/driver_inbox.php`
  - `/api/driver_ack.php`
  - `/api/driver_location_ping.php`
- heads-up/full-screen notification al entrar un viaje
- burbuja overlay sobre otras apps (si el usuario concede permiso)
- reinicio del monitoreo al encender el teléfono

## Lo que ya existe en tu backend/PWA

Tu ZIP ya trae el flujo principal:

- creación de viajes: `trip_create_ajax.php` y `trip_create_broadcast_ajax.php`
- polling/cola de chofer nativa: `api/driver_inbox.php` y `api/driver_ack.php`
- token para wrapper nativo: `api/token.php`
- ping de ubicación de chofer: `api/driver_location_ping.php`
- paneles chofer/cliente/admin y mapa en la web

## Lo que esta app sí hace

1. Abre `https://unaflecha.com`
2. El chofer inicia sesión en la web dentro de la app
3. El chofer toca **Activar nativo**
4. La app pide permisos y arranca un `ForegroundService`
5. La app obtiene `api_token` usando la sesión web del chofer
6. La app manda ubicación en segundo plano y consulta nuevos viajes cada 3 segundos
7. Si entra un viaje:
   - lo marca como notificado (`driver_ack.php`)
   - muestra notificación urgente
   - intenta abrir la app en la pantalla del viaje
   - si hay permiso overlay, muestra una burbuja flotante

## Lo que NO queda 100% garantizado solo del lado Android

### 1) Abrir automáticamente la app por encima de todo
Android moderno restringe abrir actividades arbitrariamente desde segundo plano. Esta base usa:
- full-screen intent
- notificación urgente
- overlay flotante

Eso es lo más cercano permitido por Android sin root ni hacks.

### 2) Funcionalidad “Uber completa”
Para parecerse más a Uber todavía faltaría o habría que reforzar:
- socket/websocket real en vez de polling
- navegación paso a paso nativa
- tracking continuo del pasajero también
- push robusto con Firebase si quieres mejor entrega en background extremo
- llamadas VoIP / canal de alta prioridad
- pagos nativos in-app
- módulo de estados offline y reintentos
- deep links internos más ricos en la web

## Cómo compilar

Abre esta carpeta en Android Studio Jellyfish o superior y deja que sincronice Gradle.

### Debug APK
`Build > Build APK(s)`

### Release
Configura tu keystore y firma desde Android Studio.

## Ajustes recomendados antes de producción

- cambiar `applicationId`
- poner icono adaptativo
- agregar splash screen nativa
- reemplazar polling por WebSocket o FCM
- endurecer validación de login de chofer antes de arrancar el servicio
- crear páginas web dedicadas para deep link de viaje y aceptación rápida

## GitHub Actions opcional

Se incluye un workflow base en `.github/workflows/build-debug-apk.yml`.



## Compilar desde VS Code en Windows

No necesitas Android Studio, pero si necesitas estos componentes:

- Java 17 en PATH
- Android SDK Command-line Tools
- platform-tools
- platform android-34
- build-tools 34.0.0

Este proyecto ya incluye scripts para hacerlo desde VS Code:

### 1) Instalar Java 17

En PowerShell:

```powershell
winget install Microsoft.OpenJDK.17
```

Cierra y abre VS Code despues de instalar Java.

### 2) Preparar Android CLI

En la terminal de VS Code, dentro del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-android-cli.ps1
```

### 3) Compilar la APK debug

```powershell
powershell -ExecutionPolicy Bypass -File .\scriptsuild-debug.ps1
```

### 4) Ruta del APK

La APK queda en:

```text
appuild\outputspk\debugpp-debug.apk
```

### 5) Desde el menu de VS Code

Tambien puedes usar:

- Terminal > Run Task > Android: setup CLI tools
- Terminal > Run Task > Android: build debug APK

## Nota importante sobre Gradle Wrapper

Se incluyo `gradle/wrapper/gradle-wrapper.properties`, pero si en tu PC faltara el archivo binario `gradle-wrapper.jar`, la forma mas simple es abrir una terminal en la carpeta del proyecto y ejecutar Gradle si ya lo tienes instalado globalmente, o regenerar el wrapper desde una maquina que tenga Gradle instalado.

Si te pasa ese error, dímelo y te preparo una variante con Capacitor para compilar mas facil o te doy el paso exacto para reconstruir el wrapper.
