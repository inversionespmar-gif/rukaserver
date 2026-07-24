# Diseño: Control Remoto Netflix-style en PlayerScreen

**Fecha:** 2026-07-20
**Ámbito:** `android/app/.../ui/screens/PlayerScreen.kt` y `android/app/.../player/TvPlayer.kt`
**Plataforma:** Android TV / Fire TV (Jetpack Compose + Media3 ExoPlayer)

## Problemas a resolver

El reproductor de películas/series (`PlayerScreen`) no responde a los botones
esenciales del control remoto Smart TV:

- Play/Pause no funciona con teclas hardware ni con D-pad Center.
- Flechas Izquierda/Derecha no hacen seek (+10s / -10s).
- Siguiente/Anterior episodio no responde a `KEYCODE_MEDIA_NEXT`/`PREVIOUS`.
- Los controles overlay no se muestran automáticamente al presionar teclas.

## Comportamiento objetivo

| Tecla | Controles ocultos | Controles visibles |
|-------|-------------------|-------------------|
| DPAD Center / OK | Toggle Play/Pause | Propaga (navega UI) |
| DPAD Izquierda | Seek -10s | Propaga (navega UI) |
| DPAD Derecha | Seek +10s | Propaga (navega UI) |
| MEDIA_PLAY / PAUSE / PLAY_PAUSE | Toggle Play/Pause | Toggle Play/Pause |
| MEDIA_NEXT | Siguiente episodio | Siguiente episodio |
| MEDIA_PREVIOUS | Anterior episodio | Anterior episodio |
| Cualquier tecla | Muestra controles + reset timers | Muestra controles + reset timers |

## Cambios por archivo

### `PlayerScreen.kt`

1. **Referencia reactiva a `controlsVisible` dentro del listener:**
   Usar `rememberUpdatedState(controlsVisible)` para que el `OnKeyListener`
   pueda leer el estado actualizado en cada invocación.

2. **Agregar `setOnKeyListener` al `PlayerView`:**
   Dentro del `AndroidView.factory`, después de configurar el `PlayerView`,
   llamar a `playerView.setOnKeyListener { _, keyCode, event -> ... }`.
   El listener se ejecuta **antes** que el handler interno del PlayerView.

3. **Lógica del listener:**
   - Solo actuar en `KeyEvent.ACTION_DOWN`.
   - Según `keyCode` y el estado `controlsVisible`:
     - `KEYCODE_DPAD_CENTER`: si controles ocultos → toggle play/pause (return true);
       si visibles → return false (deja pasar al PlayerView para navegación UI).
     - `KEYCODE_DPAD_LEFT`: si controles ocultos → seek -10s (true); si visibles → false.
     - `KEYCODE_DPAD_RIGHT`: si controles ocultos → seek +10s (true); si visibles → false.
     - `KEYCODE_MEDIA_PLAY_PAUSE` / `KEYCODE_MEDIA_PLAY` / `KEYCODE_MEDIA_PAUSE`:
       toggle play/pause (true).
     - `KEYCODE_MEDIA_NEXT`: avanzar al siguiente item de la queue (true).
     - `KEYCODE_MEDIA_PREVIOUS`: retroceder al anterior item de la queue (true).
   - En todos los casos, mostrar los controles:
     `playerView.showController(); controlsVisible = true`

4. **Métodos auxiliares para `next`/`prev`:**
   La activity/función encapsula `nextEpisode` y `prevEpisode` usando el estado
   `index` y la lista `queue` para cambiar de episodio y llamar `player.prepare()`.

5. **Ajuste de `skipIntro`:**
   Al cambiar de episodio manualmente, reiniciar también `showSkipIntro` (ya
   se hace en `LaunchedEffect(index)`).

### `TvPlayer.kt`

- No requiere cambios. El `player` es público y `player.playWhenReady` se puede
  togglear desde el listener en `PlayerScreen`.

## Notas de implementación

- `PlayerView.setOnKeyListener` se llama antes del dispatch interno, permitiendo
  interceptar teclas selectivamente.
- El PlayerView nativo ya maneja `KEYCODE_MEDIA_FAST_FORWARD`/`REWIND` y
  `KEYCODE_DPAD_UP`/`DOWN` para navegar la UI del controller — no hay que
  tocarlos.
- Controles ocultos vs visibles: `PlayerView.isControllerVisible()` existe pero
  es más confiable usar el callback `ControllerVisibilityListener` que ya
  actualiza `controlsVisible` en Compose.

## Criterios de aceptación

- [ ] En medio de un video, presionar OK/Enter/D-pad Center pausa/reanuda.
- [ ] Con controles visibles, OK navega los botones del overlay.
- [ ] Flecha Izquierda con controles ocultos: retrocede 10s.
- [ ] Flecha Derecha con controles ocultos: adelanta 10s.
- [ ] Con controles visibles, flechas navegan la UI del overlay.
- [ ] MEDIA_NEXT cambia al siguiente episodio (si existe en la queue).
- [ ] MEDIA_PREVIOUS cambia al anterior episodio.
- [ ] Cualquier tecla D-pad o media muestra los controles overlay.
- [ ] Skip intro se reinicia al cambiar de episodio manualmente.
