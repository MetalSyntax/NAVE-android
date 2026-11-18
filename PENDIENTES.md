# Nave — pendientes (2026-11-17)

- [x] Ícono nuevo: propuesta 9 «Velocímetro» (tablero amarillo, N blanca con diagonal roja como aguja) sobre negro `#131313` (`android/app/src/main/res/drawable/ic_launcher_*.xml`). Compila. Las 10 propuestas: `../icon-proposals/hoja.png`.
- [x] Instalado en TECNO por USB (2026-11-17) + APK copiado a /sdcard/Download (ADB Wi-Fi no llega: el teléfono no es visible desde la Mac).
- [ ] Ver el ícono en el launcher real (las marcas finas del tablero pueden perderse en tamaños pequeños; si pasa, engrosar a 4).
- [ ] Commit (sin git en esta carpeta).

## 2026-11-17 — Paridad web → Kotlin + uso en la calle

NAVE-1.0.0.apk resultó ser Compose nativo (sin assets/public ni index.html): no es una segunda versión web.

Brecha web → Kotlin
- [x] Pantalla de Ajustes completa y propia (engranaje arriba + botón en Garaje): tema, idioma, piloto, intervalo de aceite, respaldo. Persistida en DataStore.
- [x] Ajustes nuevos para la calle: alto contraste (modo sol), tamaño de texto, vibración, GPS en ruta (+ frecuencia), pantalla encendida en ruta, estado de permisos.
- [x] Resto de pantallas de la web (Tablero, Bitácora, Taller, Garaje, modales, alertas, manuales, exportar/importar) ya existían en Kotlin.
- [ ] «Restaurar ejemplo» (garaje demo de la web): no existe en Kotlin (requiere sembrar datos; borra todo).
- [ ] Vista previa de texto del manual dentro de la app (la web muestra summaryText; Kotlin abre el archivo con otra app).
- [ ] Explorador de código Kotlin y descarga del ZIP del proyecto: solo web, no aplica a la app.

Calle / GPS / fechas
- [x] Ruta con GPS real: permiso en ejecución con explicación, aviso si el GPS está apagado (lleva a Ajustes), seguimiento con servicio en primer plano (location), CSV por ruta en filesDir/tracks.
- [x] Fechas con calendario Material3 + atajos (Hoy/Ayer/Hace 2 días; póliza +6 m/+1 año/+2 años). No hay campos de hora en la app.
- [x] Botones ≥56 dp, acción principal fija abajo, steppers y atajos numéricos, háptica, TalkBack (roles, estados, encabezados), texto escalable.
- [ ] Probar en el teléfono: permisos, GPS con pantalla apagada, calendario, TalkBack, texto 150 %.
- [ ] Los puntos GPS (CSV) no entran todavía en el respaldo .zip ni hay mapa de la ruta.
- [ ] Commit (sin git en esta carpeta).

## 2026-11-17 (tarde) — Legal, perfil, ejemplo, texto, marca

- [x] Legal: `LegalScreen.kt` (Privacidad / Términos / Acerca de, tarjetas con ícono) desde Ajustes > «Privacidad y cumplimiento». Contacto = github.com/MetalSyntax (CONFIRMAR: no hay correo de soporte en el proyecto).
- [x] Perfil del motorizado: `ProfileScreen.kt` (ícono de persona en la barra superior y botón en Ajustes). Vista por defecto, Editar/Guardar; foto recortada cuadrada 512 px; cédula, teléfono, sangre, contacto de emergencia con botón Llamar (abre el marcador, sin permiso), licencia + vencimiento (aviso ≤30 días), stats de la moto activa. Persistido en DataStore (AppSettings) y dentro del respaldo (`profile` + foto). La web no tiene perfil.
- [x] «Restaurar ejemplo» (Ajustes > Datos de ejemplo, con confirmación): 2 motos + recargas/servicios/ruta; conserva ajustes y perfil.
- [x] Vista previa de manuales de texto (botón «Leer» en Garaje, solo mime text/*; PDF sigue abriéndose con otra app).
- [x] Respaldo incluye `tracks/route-<id>.csv` (export e import) y foto de perfil.
- [x] Tamaño de texto: Pequeña 90 % / Normal 100 % (defecto) / Grande / Muy grande / Máxima; el valor guardado se ajusta a la opción más cercana (`normalizeFontScale`).
- [x] Marca: barra superior = ícono + «NAVE»; Ajustes «NAVE · vX». Notificación y app_name ya eran «NAVE».
- [ ] No hecho: mapa de la ruta, SMS/compartir ubicación al contacto de emergencia, vista previa de PDF, importar CSV a la BD, tests de UI.
- [ ] Probar en el teléfono: foto (selector), llamada, vista previa, restaurar ejemplo, temas alto contraste en las pantallas nuevas.
