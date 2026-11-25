# TOWRIX — revisión y optimización (2026-11-25)

Juego: Canvas 2D en un solo archivo (`app/src/main/assets/index.html`) dentro de un WebView Android.
Se aplicaron las guías de perfilado, depuración y release del pack `threejs-game-director`
(las partes que corresponden a un juego Canvas 2D; no hay WebGL/Three.js).

## Defectos encontrados y corregidos

| # | Defecto | Impacto | Corrección |
|---|---|---|---|
| 1 | `index.html` envuelto en el esqueleto del visor de artifacts (`<!doctype html><html><head>…<body>` antes del documento real) | HTML inválido: el juego quedaba anidado dentro de otro `<body>`, con estilos y viewport duplicados | Se quitó el envoltorio; el archivo empieza con `<!DOCTYPE html>` |
| 2 | `<input type=file>` sin `onShowFileChooser` en el WebView | En la app, la bandera "Tu imagen" no abría la galería | `WebChromeClient.onShowFileChooser` + `onActivityResult` en `MainActivity` |
| 3 | Sin manejo de `pointercancel`/`blur` | Si el sistema cancela el toque, la puntería quedaba pegada | `pointercancel` y `blur` sueltan la puntería sin lanzar |
| 4 | Tipografía Bungee desde Google Fonts | La app pedía permiso de Internet; sin red cambiaba la letra del logo y los menús | Bungee (OFL 1.1, latino, 14 KB) incrustada; se quitó el permiso `INTERNET`; la licencia va en `assets/licenses/` y en los créditos |

## Rendimiento (perfilado)

Escenario fijo: viewport 412×915, DPR 2,625, CPU limitada 6× (gama baja), calidad "alta", azar con semilla,
clima lluvia, batalla activa con lanzamientos cada 0,7 s, 7 s medidos. Navegador Chromium headless
(rasterizado por software, así que los ms absolutos son pesimistas; la comparación es lo que vale).

Clasificación: limitado por relleno de píxeles (93 % del tiempo en rasterizado nativo, <7 % en JS).
DPR 1 → 37 ms/frame; DPR 2,625 → ~110 ms/frame.

| Cambio (uno a la vez, mismo escenario) | Antes (promedio / p95) | Después (promedio / p95) |
|---|---|---|
| Viñeta: canvas a media resolución re-escalado cada frame → overlay CSS compuesto | 112 / 150 · 104 / 133 · 111 / 133 ms | 71 / 100 · 83 / 100 · 70 / 100 ms |
| Arte 8/32 bits: pixelado a pantalla completa por torre → solo la franja de la torre | 74 / 117 · 88 / 133 ms | 63 / 83 · 70 / 83 ms |

La franja del pixelado se verificó píxel a píxel contra el render a pantalla completa en 4 escenas congeladas
(quieta, personaje cayendo, escudo y muralla): 0 píxeles distintos.

Optimizaciones previas (ya incluidas en el archivo que se subió al repo): logo de portada cacheado,
música clásica programada por tandas, lienzo de pixelado por tamaño, gradientes de torre cacheados,
HUD sin lecturas de layout por frame, canvas opaco y pausa y silencio al ocultar la app.

## QA (input real, Chromium móvil, red cortada)

Vertical 412×915 y horizontal 915×412, DPR 2. Resultado idéntico en ambas:

- Intro → portada → menú por toques: OK
- Bungee cargada sin red (`document.fonts.check`): sí · peticiones externas: 0 · errores de consola: 0
- Taller → bandera con imagen propia por el selector de archivos: OK
- Partida rápida: 14 piezas colocadas tocando la grilla, largada, lanzamiento arrastrando: OK
- `pointercancel` suelta la puntería: OK
- `onAndroidPause` pausa y muestra el menú de pausa; atrás reanuda: OK
- Victoria (+102 monedas) → REVANCHA: estado limpio (0 proyectiles, 0 partículas)
- Atrás con una ventana abierta la cierra: OK

## Android

- `MainActivity.java` compilado con `javac --release 17` contra el framework de Android 14
  (`org.robolectric:android-all:14-robolectric-10818077`): sin errores.
- **No se generó el APK/AAB**: la red del entorno bloquea `dl.google.com` (SDK de Android).
  Pendiente: compilar en Android Studio y probar la bandera con imagen en un celular real.
- Depuración remota del WebView activada solo en builds debug (`FLAG_DEBUGGABLE`).
- El WebView no navega fuera de `file:///android_asset/`.

## Sin cambiar (decisión)

- R8/`shrinkResources`: la app casi no tiene código Java, así que la ganancia sería mínima y no se pudo compilar para verificarlo.
- Íconos PNG: `aapt2` ya los comprime al generar el release.
- `versionCode` 1 / `versionName` 0.6: hay que subirlos antes de publicar una actualización en Play.
