# Diseño: Resume Playback (Netflix-style) en PlayerScreen

**Fecha:** 2026-07-20
**Ámbito:** Nuevo archivo `PlaybackProgressStore.kt`, modificar `PlayerScreen.kt` y `HomeScreen.kt`
**Plataforma:** Android TV / Fire TV (Jetpack Compose + DataStore + Media3 ExoPlayer)

## Problema a resolver

Cuando un usuario ve una película o serie y sale antes de terminarla, la app no
recuerda dónde se quedó. Al volver a reproducir el mismo contenido, debe poder
elegir continuar desde donde lo dejó o empezar desde el principio.

## Arquitectura

### `PlaybackProgressStore.kt` (nuevo archivo)

Ubicación: `data/local/PlaybackProgressStore.kt`
Almacenamiento: DataStore Preferences (ya se usa para `CredentialsStore`).

Estructura de datos guardada: un único string JSON que mapea URLs a posición en ms.

```kotlin
class PlaybackProgressStore(private val context: Context) {
    private val DataStore by preferencesDataStore(name = "playback_progress")
    private val progressKey = stringPreferencesKey("progress")

    // Flow de posición para una URL específica (null si no hay progreso)
    fun getProgress(url: String): Flow<Long?>

    // Guardar posición (en ms)
    suspend fun saveProgress(url: String, positionMs: Long)

    // Eliminar progreso de una URL
    suspend fun removeProgress(url: String)
}
```

Internamente guarda un JSON: `{"url1": 123456, "url2": 789012}`.

### `PlayerScreen.kt` (modificar)

Se añade un nuevo parámetro `progressStore: PlaybackProgressStore`.

**Flujo al abrir un video** (`LaunchedEffect(index)`):
1. Obtener el `PlayItem` actual vía `queue[index]`.
2. Llamar `progressStore.getProgress(url).first()` (una sola vez, no como Flow continuo).
3. Si hay progreso guardado Y el progreso es < 90% de la duración total →
   mostrar overlay "Continuar viendo?".
4. Si no hay progreso o el video está >90% completado → empezar normalmente.

**Overlay "Continuar viendo?"** (`AnimatedVisibility`):
- Aparece al inicio, encima del video, centrado.
- Texto: "Continuar viendo?"
- Timestamp: muestra el tiempo guardado formateado (ej. "13:42").
- Botón "Continuar": `player.seekTo(savedPos)`, ocultar overlay.
- Botón "Empezar de nuevo": `progressStore.removeProgress(url)`, ocultar overlay.
- Si no se acciona en 5 segundos → auto-descartar (comienza desde el principio).

**Guardado periódico** (`LaunchedEffect`):
- Cada 5 segundos mientras el video se está reproduciendo
  (`player.isPlaying`), guardar `player.currentPosition`.
- También guardar al pausar (en el `Player.Listener.onPlaybackStateChanged` o
  cuando `playWhenReady` cambia a false).
- Guardar al salir de la pantalla (`DisposableEffect.onDispose`).

**Marcar como completado**: Cuando el video llega al final
(`Player.STATE_ENDED`), eliminar el progreso con `removeProgress(url)`.

### `HomeScreen.kt` (modificar)

Instanciar `PlaybackProgressStore` y pasarlo a la llamada a `onPlayQueue`
que lleva a `PlayerScreen`.

## Criterios de aceptación

- [ ] Al abrir una película/episodio con progreso guardado, aparece overlay
      "Continuar viendo?" con el timestamp formateado.
- [ ] "Continuar" reanuda desde la posición guardada.
- [ ] "Empezar de nuevo" borra el progreso y comienza desde 0.
- [ ] Si no se acciona en 5s, el overlay se cierra y el video empieza desde 0.
- [ ] El progreso se guarda cada 5s durante la reproducción.
- [ ] El progreso se guarda al pausar.
- [ ] Al terminar el video (>90% o STATE_ENDED), se limpia el progreso.
- [ ] Series: cada episodio tiene su propio progreso (por URL).
- [ ] No afecta Live TV.
