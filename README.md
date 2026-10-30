# Fake Bad Signal

Dos apps Android en un mismo proyecto:

## 1. Fake Bad Signal (`fakebadsignal/`)
Simula mala cobertura en las llamadas con los contactos que elijas.
- Eliges contactos en una pantalla propia; el efecto se activa solo al llamar o recibir su llamada y se para al colgar.
- Corta tu micrófono a ráfagas aleatorias, así esa persona te oye entrecortado.
- Opcional: estática por el altavoz (solo se capta con la llamada en altavoz).
- Botón de ejemplo: graba 8 s de tu voz y reproduce cómo te oirá la otra persona.
- Intensidad ajustable.

Configuración (una vez): conceder permisos y permitir "detectar con quién llamas" (rol de filtro de llamadas; la app nunca bloquea ni modifica llamadas).

Limitación: Android no permite alterar el audio de una llamada sin root. El corte usa el silencio del micrófono del sistema, que en algunos móviles/versiones no tiene efecto. Probar primero con un amigo.

## 2. Background Sounds (`backgroundsounds/`)
Sonidos de ambiente en bucle (título de trabajo, se puede renombrar): 16 grabaciones reales (mar, playa, gaviotas, lluvia, viento, río, bosque, ranas, hoguera, fuego, tráfico, tienda, granja, aire acondicionado, ritual, misterio) y 5 sintetizados (cafetería, misa, aeropuerto, tenis, fútbol). Suenan con la pantalla apagada, con volumen y temporizador.

Las grabaciones están en `backgroundsounds/src/main/res/raw/` (mono, 22 kHz, máx. 60 s), descargadas de Pixabay (licencia Pixabay, sin atribución obligatoria; comprueba las condiciones antes de publicar).

## Idiomas
Ambas apps se traducen solas al idioma del sistema: inglés (por defecto), español, francés, alemán, italiano y portugués. Los textos están en `res/values*/strings.xml`; para añadir un idioma, crea `values-xx/strings.xml` y añádelo a `res/xml/locales_config.xml`. En Android 13+ cada app también aparece en Ajustes > Idiomas de las apps.

## Compilar
Android Studio, o `gradle assembleDebug` (genera un APK por app). GitHub Actions deja ambos APK como artefacto.

## Publicar en Google Play (AAB firmado)
El workflow `Release AAB (Google Play)` (Actions > Run workflow, o al subir una etiqueta `v1.0`) genera un `.aab` por app, firmado con tu clave de subida.

Secrets necesarios (Settings > Secrets and variables > Actions > New repository secret):

| Secret | Contenido |
|---|---|
| `KEYSTORE_BASE64` | el archivo `.jks` codificado en base64 |
| `KEYSTORE_PASSWORD` | contraseña del keystore |
| `KEY_ALIAS` | alias de la clave (`upload`) |
| `KEY_PASSWORD` | contraseña de la clave |

El `versionCode` sube solo con cada ejecución (número de run). Las apps apuntan a Android 16 (API 36), requisito de Google Play desde el 31/08/2026.
Guarda una copia del keystore fuera de GitHub. Con Play App Signing, si la pierdes, Google permite restablecer la clave de subida.
