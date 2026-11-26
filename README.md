# TOWRIX

Juego de **New Player One**: construyes tu torre vista desde arriba, te subes a la cima y, cuando se apagan las luces de largada, derribas la bandera rival en una batalla en tiempo real.

- **Versión actual:** 1.0 · versionCode 1 (primera versión para Google Play)
- **Paquete:** `com.newplayerone.towrix` (provisorio, ver nota abajo)
- **Plataformas:** Android (contenedor WebView nativo, sin Capacitor ni npm). iOS más adelante.
- **Prototipo jugable (privado):** https://claude.ai/artifact/9ev1p5yQgcGAR4uSbiEHZM

## Estructura

| Ruta | Qué es |
|---|---|
| `app/src/main/assets/index.html` | El juego completo (HTML5 canvas, un solo archivo, con la tipografía incluida) |
| `app/src/main/assets/licenses/` | Licencia de la tipografía Bungee (SIL OFL 1.1), va dentro de la app |
| `app/src/main/java/com/newplayerone/towrix/Ads.java` | Videos con recompensa (AdMob) y consentimiento (UMP), expuestos al juego como `AndroidAds` |
| `app/src/main/java/com/newplayerone/towrix/MainActivity.java` | Contenedor WebView de Android: pantalla completa, guardado local, botón atrás, pausa al salir, notch y selector de imagen para la bandera |
| `app/src/main/res/mipmap-*/` | Ícono de la app (adaptable, redondo y clásico) |
| `app/build.gradle` | Paquete, versión y SDK (minSdk 24, targetSdk 36) |

## Qué incluye el juego (v1.0)

- **Modo historia:** 5 mundos (Obra Central, Bosque Leñador, Glaciar Eterno, Volcán Furioso, Ciudad Neón), un jefe final cada 10 etapas y hasta 3 estrellas por etapa.
- **Habilidades especiales** que se ganan venciendo jefes: Tiro triple, Reparar, Terremoto, Meteoro e Imán.
- **Partida rápida** con 4 dificultades.
- **Taller:** 17 tipos de personaje, ropa, sombreros, lentes, capas, herramientas con poder, mascotas con poder, banderas y celebraciones.
- **Clima de la arena** que cambia durante la batalla y potencia o debilita los poderes.
- **Calidad gráfica** automática, alta o ahorro.

## Anuncios (videos con recompensa)

El juego no muestra anuncios por su cuenta: solo ofrece videos **opcionales** que dan premio.
- **Resultado de una partida:** "📺 Duplicar monedas" (una vez por partida).
- **Taller / Materiales:** cuando te faltan monedas para comprar, "📺 Video: +50 🪙" (máximo 5 al día).
- **Menú → Privacidad:** aparece solo cuando la ley lo exige (Europa/Reino Unido) para cambiar el consentimiento.

En el navegador (prototipo) no hay anuncios: los botones solo aparecen dentro de la app.

**Antes de publicar** pon tus IDs reales en `gradle.properties` (`TOWRIX_ADMOB_APP_ID` y `TOWRIX_ADMOB_REWARDED_ID`).
Hoy tienen los IDs de prueba de Google. Los builds *debug* usan siempre los de prueba, para que nunca toques tus anuncios reales.
En AdMob crea también el mensaje de consentimiento (Privacidad y mensajes → GDPR) y publica `app-ads.txt` en el sitio web de desarrollador que declares en Play.

## Compilar

Abrir la carpeta en Android Studio → *Sync Now* → **Build › Generate Signed App Bundle**.

La llave de firma (`.jks`) **nunca** se sube a este repositorio (está en `.gitignore`).

## Notas

- **Nombre de paquete:** `com.newplayerone.towrix` se puede cambiar en `app/build.gradle` mientras la app no esté subida a Google Play. Después de subirla queda fijo para siempre.
- **Orientación:** el juego se puede usar en vertical y en horizontal. La batalla se ve más grande en horizontal.
- **Tipografía:** Bungee va incrustada en `index.html` (subconjunto latino, 14 KB). La app no pide ningún permiso y funciona completa sin internet.
- **Depurar en un celular:** en builds de prueba (debug) el WebView se puede inspeccionar desde `chrome://inspect` en Chrome de escritorio. En la versión publicada está desactivado.
- **Actualizar el juego desde el prototipo:** si copias el HTML desde el artifact de Claude, revisa que el archivo empiece con `<!DOCTYPE html>`; el visor puede agregar un envoltorio `<html><head>…<body>` que hay que quitar.
- **Botón atrás de Android:** en partida pausa, con una ventana abierta la cierra, en el menú vuelve a la portada y en la portada cierra la app.

© New Player One
