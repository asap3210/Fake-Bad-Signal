# Fake Bad Signal – datos para Google Play Console

## Ficha (EN)
- **Title (≤30):** Fake Bad Signal
- **Short description (≤80):** Make selected contacts hear you breaking up, as if you had no signal.
- **Full description:**
  Fake Bad Signal simulates a bad phone connection during calls with the contacts you choose, so you can wrap up a call with a believable "you're breaking up".

  How it works
  • Pick the contacts that should get the effect.
  • When you call them (or they call you), the app turns on by itself and briefly mutes your microphone at random moments.
  • Optional static through the speaker (only picked up if the call is on speaker).
  • Adjust the intensity and hear an 8-second preview of how you will sound.

  Permissions, explained
  • Contacts: to choose who gets the effect. Read on your device only.
  • Phone state / call detection: to know when a call starts and ends and with whom.
  • Microphone: only for the optional preview. Nothing is saved or sent.
  • Notifications: shows a notice while the effect is active.

  No ads. No data leaves your phone. The app never blocks or records your calls.
  Note: Android does not let apps change call audio directly. The effect works by muting your own microphone, which may not work on every phone.

## Ficha (ES)
- **Título:** Fake Bad Signal
- **Descripción corta:** Haz que los contactos que elijas te oigan entrecortado, como si no tuvieras cobertura.
- **Descripción completa:** equivalente a la anterior (traducir).

## Declaración de permisos / Prominent disclosure
La app muestra un aviso destacado dentro de la app **antes** de pedir contactos, estado del teléfono, notificaciones y micrófono (ver `MainActivity.kt`, diálogos "core" y "mic").
**No** usa `READ_CALL_LOG`, `READ_SMS` ni permisos de SMS/registro de llamadas, así que no necesita el formulario de esos permisos restringidos.
Usa el rol `ROLE_CALL_SCREENING` (CallScreeningService) solo para conocer el número de la llamada; siempre responde "permitir" sin bloquear nada.

## Seguridad de los datos (Data safety)
- Recopila datos: **No**. (Contactos y estado de llamada se procesan solo en el dispositivo y no salen de él; el audio de la vista previa no se guarda ni se envía.)
- Comparte datos: **No**.
- Cifrado en tránsito: no aplica (sin tráfico de red).
- Eliminación de datos: los ajustes se borran al desinstalar o borrar los datos de la app.
- Anuncios: **No contiene anuncios**.
- Política de privacidad (URL): https://github.com/asap3210/Fake-Bad-Signal/blob/main/PRIVACY.md


## Servicio en primer plano
- Tipo: `mediaPlayback` (CallService).
- Para qué: mantener activo el efecto durante una llamada con un contacto elegido (reproduce estática opcional y alterna el micrófono) mostrando una notificación visible; se detiene al colgar.
- Vídeo de demostración: Play suele pedir un vídeo corto que muestre: elegir un contacto → llamar → ver la notificación "Fake Bad Signal está activo" → colgar y ver que desaparece.

## Otros
- Público objetivo: adultos (no dirigido a menores).
- Anuncios: no.
- Categoría sugerida: Herramientas o Entretenimiento.
