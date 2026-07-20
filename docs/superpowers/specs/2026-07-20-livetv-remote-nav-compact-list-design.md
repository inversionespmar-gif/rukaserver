# Diseño: Navegación con control remoto y lista compacta en Live TV (fullscreen)

**Fecha:** 2026-07-20
**Ámbito:** `android/app/.../ui/screens/LiveTvScreen.kt` y `android/app/.../ui/components/ChannelRow.kt`
**Plataforma:** Android TV / Fire TV (Jetpack Compose)

## Problemas a resolver

1. **Foco del control remoto en el overlay de canales (fullscreen).** Cuando la lista
   de canales está visible sobre el reproductor a pantalla completa, las flechas
   Arriba/Abajo del mando no navegan por los canales y el overlay no acepta el foco.
   Objetivo: al abrir la lista, el foco del mando entra en ella; Arriba/Abajo seleccionan
   canales y OK sintoniza el canal seleccionado y oculta la lista.

2. **Diseño demasiado ancho de la lista.** Los elementos de la lista ("líneas grises")
   usan `fillMaxWidth()` dentro de un overlay que ocupa toda la pantalla, extendiéndose
   casi por todo el ancho. Objetivo: lista compacta, anclada a la derecha, ancho fijo
   (~400dp), con la línea gris recortada a ese ancho.

## Causa raíz

En `LiveTvScreen.kt` (modo fullscreen, líneas ~149-191):
- El `Box` contenedor del overlay tiene `.clickable { overlay = false }` y
  `fillMaxSize()`, absorbiendo el foco y los eventos de teclado.
- El `LazyColumn` no está marcado como enfocable ni solicita foco al abrirse.
- `ChannelRow` usa `fillMaxWidth()` y el overlay ocupa toda la pantalla → líneas anchas.

## Comportamiento objetivo

- En fullscreen: un **clic/tap** o la tecla **OK/Enter** sobre el video abre la lista
  de canales anclada a la derecha (ancho ~400dp, alto completo, scrollable).
- Al abrir, el foco del mando entra en el `LazyColumn` y la selección arranca en el
  canal actualmente sintonizado (`selectedIndex`).
- **Arriba/Abajo** mueven la selección; al enfocar cada `ChannelRow` se previsualiza el
  canal (`onFocus = { playIndex(i) }`, ya existente).
- **OK/Enter** sobre un canal → `onClick = { playIndex(i); overlay = false }` (ya existe).
- **Back** cierra el overlay y vuelve al video (ya implementado en `BackHandler`, línea ~140).
- La "línea gris" de cada canal queda compacta al ancho de la lista (no a pantalla completa).

## Cambios por archivo

### `LiveTvScreen.kt` (sección fullscreen)

1. **Abrir con teclado:** el `Box` que hoy solo captura clic para abrir/cerrar el
   overlay se amplía con `onKeyEvent`. En `KeyEventType.KeyDown`, `Key.Enter` /
   `Key.DirectionCenter` → `overlay = !overlay` (adicional al `clickable` existente).
2. **Restructurar el overlay:**
   - Contenedor de fondo semitransparente `fillMaxSize()` que cierra el overlay solo
     con clic **fuera** de la lista (`clickable { overlay = false }`), sin robar el foco
     de la lista.
   - `LazyColumn` anclado a la derecha:
     `Modifier.width(400.dp).fillMaxHeight().align(Alignment.CenterEnd)` (o equivalente),
     con `verticalArrangement = spacedBy`, marcado `focusable()` y con un `LazyListState`
     para hacer scroll automático al item enfocado.
3. **Mover el foco al abrir:** `LaunchedEffect(overlay)` cuando `overlay == true` →
   solicitar foco al `LazyColumn` (p. ej. vía `FocusRequester`) y hacer
   `listState.scrollToItem(selectedIndex)` para que la selección inicie en el canal
   sintonizado.

### `ChannelRow.kt`

- Reemplazar `Modifier.fillMaxWidth()` por un ancho que respete el contenedor padre
  (el `LazyColumn` ya limita a 400dp). Usar `Modifier.fillMaxWidth()` **dentro** del
  contenedor acotado, o bien `Modifier.widthIn(max = 400.dp)`, de modo que la "línea
  gris" (fondo `Surface` redondeado) quede recortada al ancho de la lista.
- Conservar intacto el estado visual de foco existente (borde `Accent`, fondo
  `Accent.copy(alpha=0.12f)`, badge, flecha `▶`), ya que cumple con el requisito de
  estado "enfocado" claro.

## Notas de implementación

- Se reusa `TvPlayer`, `LiveTvViewModel`, `ChannelRow` y la lógica de `playIndex` ya
  existentes; no se introducen nuevos componentes salvo `FocusRequester` y el
  `LazyListState` dedicado al overlay.
- No afecta la vista split (no-fullscreen), que ya funciona con foco nativo.

## Criterios de aceptación

- [ ] En fullscreen, OK/Enter o clic abre la lista de canales.
- [ ] Con la lista abierta, Arriba/Abajo mueven la selección resaltada.
- [ ] OK sobre un canal lo sintoniza y cierra la lista.
- [ ] Back cierra la lista y devuelve el foco al video.
- [ ] La lista está anclada a la derecha con ancho ~400dp y la línea gris es compacta.
- [ ] El estado enfocado de cada canal es claramente visible.
