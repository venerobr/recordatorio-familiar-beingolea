# APK nativo — Familia Beingolea

Este proyecto crea una app Android ligera. Los cumpleaños continúan guardados en la misma base D1 de la web; el servidor revisa la fecha diariamente y OneSignal entrega el aviso a los teléfonos con la app instalada. La app no se queda ejecutándose ni consulta la fecha continuamente.

## Preparación única de las notificaciones Android

1. En [Firebase Console](https://console.firebase.google.com/), crea un proyecto (o usa uno familiar) y registra una app Android con este identificador exacto: `com.beingolea.recordatorios`.
2. En Firebase, crea una clave de cuenta de servicio para Firebase Cloud Messaging (FCM). Descárgala y guárdala solo en tu computadora; **no la compartas en el chat ni la subas a GitHub**.
3. En OneSignal abre la app **Nevensa App** > **Settings** > **Push & In-App** > **Google Android (FCM)**. Sube el JSON de cuenta de servicio y termina el asistente. Esto añade Android a la app OneSignal que ya envía los avisos web.
4. En OneSignal, confirma que la app Android esté habilitada para recibir push. El App ID de esta app ya está incluido en el proyecto; no es la clave REST.

## Publicar el cambio de envío

El Worker ya está preparado localmente para mandar el enlace adecuado a cada tipo de suscripción, abrir la app con el saludo y usar el canal Android de importancia alta. Para que el Worker publicado use esos cambios, después de completar Firebase/OneSignal abre PowerShell en la carpeta principal `recordatorio-familiar` (la carpeta que contiene `wrangler.toml`) y ejecuta:

~~~powershell
npx.cmd wrangler deploy
~~~

Este comando actualiza el Worker en Cloudflare; no cambia la base de datos ni elimina los cumpleaños.

La configuración de FCM es obligatoria para que OneSignal entregue notificaciones Android. La aplicación comparte el identificador familiar de cada persona con la suscripción web, así que el backend actual conserva la exclusión del cumpleañero.

## Generar el APK en línea sin instalar Android Studio

El repositorio incluye un flujo de GitHub Actions en `.github/workflows/android-apk.yml`. GitHub prepara Java, Gradle y Android SDK en la nube y deja el APK como archivo descargable. Para que las compilaciones de GitHub Actions sean gratuitas sin depender de una cuota privada mensual, el repositorio debe ser público. El código no contiene la clave REST de OneSignal ni las credenciales FCM; no subas la carpeta `.wrangler` ni archivos de cuentas de servicio. GitHub Actions ofrece sus runners estándar sin costo para repositorios públicos.

1. Crea una cuenta gratuita en GitHub y publica esta carpeta `recordatorio-familiar` como un repositorio público. Si usas GitHub Desktop, el `.gitignore` ya excluye `.wrangler`, archivos de compilación y credenciales JSON.
2. En GitHub abre la pestaña **Actions** y el flujo **Crear APK Android**.
3. Pulsa **Run workflow** y espera a que termine.
4. Abre esa ejecución, descarga el artefacto `cumpleanos-beingolea-apk` y extrae `app-debug.apk`.

El APK de depuración sirve para instalarlo y probarlo en los teléfonos. Configura Firebase/FCM en OneSignal antes de probar la recepción real de push.

## Generar el APK con Android Studio (alternativa)

Esta computadora no tiene Android Studio, Java ni Gradle instalados. Después de instalar Android Studio (versión Meerkat o posterior) y aceptar que descargue Android SDK 36:

1. Abre Android Studio > **Open** y selecciona esta carpeta `android`.
2. Espera a que termine **Gradle Sync**.
3. Selecciona **Build > Build APK(s)**.
4. El APK de prueba queda en `app/build/outputs/apk/debug/app-debug.apk`.
5. Comparte ese APK con cada familiar para que lo instale; en su Android deberán permitir la instalación desde esa fuente y aceptar las notificaciones.

Para probar, instala el APK en un Android con Google Play Services, abre la app, ingresa el código familiar, elige el nombre del dueño del teléfono y pulsa **Activar notificaciones**. En OneSignal aparecerá la suscripción Android. Luego envía un test push a esa suscripción desde OneSignal.

## Aviso emergente y sonido

La app crea el canal `Cumpleaños de la familia` con importancia alta y vibración. Android permite que cada usuario ajuste ese canal, el sonido, las ventanas emergentes y las notificaciones de pantalla bloqueada desde Ajustes. El sistema también puede retrasar avisos si se desactivan las notificaciones, se fuerza la detención de la app o el fabricante aplica restricciones agresivas de batería.

## Parámetros del proyecto

- Paquete Android: `com.beingolea.recordatorios`
- API familiar: el mismo Worker ya publicado
- Identidad OneSignal: el UUID interno del familiar (el mismo que la suscripción web)
- Clave REST de OneSignal: nunca se guarda en el APK
- `google-services.json` / llaves FCM: no se incluyen en este proyecto
