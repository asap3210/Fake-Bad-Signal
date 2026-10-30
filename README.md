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

## Compilar
Android Studio, o `gradle assembleDebug` (genera un APK por app). GitHub Actions deja ambos APK como artefacto.
