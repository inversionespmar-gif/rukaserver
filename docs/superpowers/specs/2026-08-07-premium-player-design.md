# Diseño: Reproductor Premium RukaTV

## Resumen

Mejora del reproductor básico de RukaTV a un diseño premium estilo Netflix, compatible con navegación por control remoto D-pad para Android TV/Smart TV.

## Decisiones de Diseño

### Layout: Bottom Toolbar (Estilo Netflix)
- Controles principales en la parte inferior
- Acciones extra (captura, favorito, configuración) en la parte superior derecha
- Overlay semi-transparente con gradientes suaves
- Título del contenido en la parte superior

### Funcionalidades Premium
1. **Subtítulos & Audio** - Selector de pistas de audio y subtítulos con estilos personalizables
2. **Velocidad de Reproducción** - Control de velocidad: 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x
3. **Calidad de Video** - Selector de resolución: Auto, 480p, 720p, 1080p, 4K (si disponible)
4. **Picture-in-Picture (PiP)** - Modo ventana flotante para continuar viendo mientras navegas
5. **Temporizador de Sueño** - Apagar automáticamente: 15min, 30min, 45min, 1h, 2h
6. **Capturas de Pantalla** - Tomar capturas del video con un botón o gesto

### Navegación D-pad
- Todos los elementos interactivos son navegables con D-pad
- Indicadores de foco visibles (borde azul brillante + sombra glow)
- Orden de navegación lógico: izquierda → derecha, arriba → abajo
- Zonas: Acciones (arriba) ↔ Controles (abajo)

## Estructura de Archivos a Modificar

### `PlayerScreen.kt`
- Refactorizar overlay de controles para incluir nuevos botones de acción
- Agregar menús desplegables para: subtítulos, audio, velocidad, calidad
- Implementar navegación D-pad completa con FocusRequester
- Agregar estados de foco visibles en todos los elementos interactivos

### Archivos Nuevos
- `PlayerOverlay.kt` - Componente reutilizable para el overlay del reproductor
- `PlayerMenus.kt` - Menús desplegables (subtítulos, audio, velocidad, calidad)
- `PlayerActions.kt` - Botones de acción (captura, favorito, PiP, temporizador)

## Flujo de Navegación D-pad

```
┌─────────────────────────────────────────────────────────────┐
│  [Título del contenido]                    [📷] [♡] [⚙]    │
│                                                             │
│                        [▶ PLAY]                             │
│                                                             │
│  [28:12] ═══════════●═════════════════════════════ [55:20]  │
│                                                             │
│  [⏮] [⏪] [▶ PLAY] [⏩] [⏭]     [CC] [1x] [HD] [⊡] [⏾]  │
└─────────────────────────────────────────────────────────────┘
```

### Orden de Foco (D-pad)
1. Botón Anterior (⏮)
2. Botón Retroceder (⏪)
3. Botón Play/Pause (▶) - Centro
4. Botón Adelantar (⏩)
5. Botón Siguiente (⏭)
6. Botón Subtítulos (CC)
7. Botón Velocidad (1x)
8. Botón Calidad (HD)
9. Botón PiP (⊡)
10. Botón Temporizador (⏾)
11. Botón Captura (📷) - Zona superior
12. Botón Favorito (♡) - Zona superior
13. Botón Configuración (⚙) - Zona superior

### Atajos de Teclado (D-pad)
- **← →** Mover entre botones principales
- **↑ ↓** Mover entre zonas (acciones ↔ controles)
- **OK/Enter** Seleccionar / Play-Pause
- **Back** Cerrar menús / Volver

## Estados de Foco

### Elemento Enfocado
- Borde: 2px sólido `#00D4FF`
- Sombra: `box-shadow: 0 0 12px rgba(0,212,255,0.5)`
- Fondo: `rgba(0,212,255,0.3)`

### Elemento No Enfocado
- Borde: 2px sólido `transparent`
- Sin sombra
- Fondo: `rgba(255,255,255,0.15)`

## Colores del Tema

```kotlin
// Acentos del reproductor
val PlayerAccent = Color(0xFF00D4FF)      // Azul eléctrico
val PlayerFocused = Color(0x3300D4FF)     // Fondo enfocado
val PlayerBorder = Color(0xFF00D4FF)      // Borde enfocado
val PlayerGlow = Color(0x8000D4FF)        // Sombra glow

// Superficies
val PlayerOverlay = Color(0xBB000000)     // Overlay semi-transparente
val PlayerGradient = Brush.verticalGradient(
    listOf(Color(0xBB000000), Color.Transparent)
)
```

## Componentes UI

### Barra de Progreso
- Altura: 6px (normal), 8px (con foco)
- Thumb: 16px con borde blanco
- Color: `#00D4FF`
--seeking habilitado con D-pad (izquierda/derecha)

### Botones de Acción
- Tamaño: 36px x 36px (acciones), 48px x 48px (play)
- Bordes redondeados: 6px (acciones), 50% (play)
- Iconos: 14px (acciones), 20px (play)

### Menús Desplegables
- Posición: sobre el botón que los activó
- Ancho: 180px
- Altura: maxHeight 240px con scroll
- Navegación: ↑ ↓ para seleccionar, OK para confirmar, Back para cerrar

## Compatibilidad

- **Android TV / Google TV** - Navegación D-pad completa
- **Teléfonos Android** - Touch + gestos preservados
- **Tablets** - Layout adaptativo según orientación

## Pruebas

1. Navegar por todos los botones con D-pad sin perder foco
2. Abrir/cerrar cada menú con D-pad
3. Seleccionar opciones dentro de menús con D-pad
4. Verificar que el foco se guarde al cambiar de zona
5. Probar en dispositivo Android TV real o emulador con D-pad
