# Recordatorios de cumpleaños — Familia Beingolea

Sitio web instalable para Android que registra cumpleaños y manda una notificación push automática a los familiares inscritos. No pide correos ni teléfonos. El cumpleañero queda excluido del envío aunque se marque por error como destinatario. El push muestra el saludo; al tocarlo abre la página con el texto y el enlace del grupo para compartirlo manualmente en WhatsApp.

## Qué hace falta para ponerlo en línea

- Una cuenta gratuita de Cloudflare para alojar el sitio, guardar los datos en D1 y ejecutar la revisión diaria.
- Una cuenta gratuita de OneSignal configurada para Web Push. El plan gratuito vigente permite Web Push; sus condiciones pueden cambiar.
- Que cada familiar abra la web en su teléfono, seleccione su nombre y acepte las notificaciones. En Android se recomienda Chrome y no usar incógnito.
- Un administrador que registre cada cumpleaños y pegue el enlace de invitación al grupo.

No es necesario que los familiares tengan Gmail, correo o número guardado. Sí necesitan conexión a internet, un navegador compatible y permitir las notificaciones. El servicio no envía mensajes automáticamente al chat de WhatsApp.

## Publicación (una sola vez)

1. Instala Node.js en la computadora y abre PowerShell en esta carpeta.
2. Crea una base D1 gratuita desde la terminal:

   ~~~powershell
   npx wrangler login
   npx wrangler d1 create recordatorio-familiar
   ~~~

3. Copia el identificador de base de datos que devuelve el comando y reemplaza `REEMPLAZAR_CON_ID_DE_D1` en `wrangler.toml`.
4. Aplica el esquema:

   ~~~powershell
   npx wrangler d1 migrations apply recordatorio-familiar --remote
   ~~~

5. En OneSignal crea una app con Web Push. Configura como sitio la URL que Cloudflare asignará al Worker (`https://...workers.dev`). Copia el App ID y la REST API Key desde OneSignal.
6. Crea claves privadas y guárdalas como secretos. Usa una clave de administración larga y un código de acceso familiar distinto:

   ~~~powershell
   npx wrangler secret put ONESIGNAL_APP_ID
   npx wrangler secret put ONESIGNAL_REST_API_KEY
   npx wrangler secret put ADMIN_PIN
   npx wrangler secret put JOIN_CODE
   npx wrangler secret put PUBLIC_URL
   ~~~

   `PUBLIC_URL` debe ser la URL HTTPS final del Worker. `ONESIGNAL_APP_ID` se mantiene en el servidor y la web recibe únicamente el identificador público que necesita el SDK.
7. Publica:

   ~~~powershell
   npx wrangler deploy
   ~~~

8. Abre la URL publicada en Chrome Android, entra a **Administración familiar**, crea familiares y cumpleaños, pega el enlace de invitación de WhatsApp y guarda.
9. Comparte la URL y el código familiar con cada persona. Cada una elige su nombre y toca **Activar avisos**. La primera prueba de notificaciones conviene hacerla desde OneSignal con un mensaje de prueba antes de confiarle los cumpleaños.

## Uso

- El Worker revisa las fechas cada día a las 8:00 a. m. de Perú (13:00 UTC).
- En el cumpleaños, OneSignal envía el saludo como push a los dispositivos inscritos de los destinatarios elegidos.
- El destinatario toca el push para ver el saludo y abrir el grupo. Debe tocar enviar en WhatsApp.
- Los datos guardados son nombres, día y mes, saludos y selección de destinatarios. No se almacena año de nacimiento, correo ni teléfono.

## Ajustes importantes

- El código de acceso familiar permite ver nombres y abrir los saludos compartidos. No lo publiques en redes.
- La clave de administración da acceso a cumpleaños y al enlace de invitación del grupo. No la compartas.
- El endpoint de push usa la REST API Key solo en el Worker; nunca la pongas en archivos de la carpeta `public`.
- Si alguien cambia de teléfono, debe abrir la página otra vez y activar avisos en el nuevo dispositivo.
- La plataforma depende de Cloudflare, OneSignal y los permisos/notificaciones del teléfono. Un push puede no mostrarse si se desactivan los permisos, el teléfono está sin conexión o el navegador limita la actividad.

## Archivos

- `public/`: interfaz en español, manifest, icono y service worker de OneSignal.
- `src/worker.js`: API, protección de administración, almacenamiento D1 y tarea programada.
- `migrations/0001_init.sql`: esquema de base de datos.
- `wrangler.toml`: configuración para publicar en Cloudflare Workers.
