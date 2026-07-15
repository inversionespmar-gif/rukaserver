# RukaTV — Android TV IPTV Player

App nativa Android TV (Kotlin + Jetpack Compose) estilo IPTV Smarters para el servidor Rukaserver (Xtream Codes).

## Requisitos
- Android Studio (Hedgehog o superior)
- minSdk 21 (Android TV / Google TV). Dispositivo o emulador con imagen de TV.
- Acceso a internet.

## Abrir y compilar
1. `git clone` de este repo y abrilo en Android Studio.
2. Esperá la sincronización de Gradle.
3. Conectá un Android TV / Google TV por ADB (`adb connect <ip>:5555`) o usá el emulator con TV image.
4. Run `app` (botón play) o `./gradlew assembleRelease` y sideload el APK.

## Uso
- En el login ingresá **Host** (ej. `https://rukaserver-1.onrender.com`), **Usuario** y **Contraseña**.
- TV en vivo: lista enumerada; mové el foco para auto-reproducir, escribí el número o buscá por nombre.
  Clic en el reproductor → pantalla completa. Clic en pantalla completa → overlay de canales sobre el video.
- Películas / Series: grilla → detalle → reproducir.
- Buscar y Favoritos (★) incluidos.

## Notas
- Las credenciales y favoritos se guardan localmente (DataStore).
- El reproductor usa Media3 ExoPlayer (HLS nativo).
