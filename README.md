# Baby Registry 👶

App Android para registrar el día a día del bebé: tomas, sueño, pañales y más.
Hecha en **Kotlin + Jetpack Compose** (Material 3), funciona sin conexión.

## Funciones

- **Pecho** con temporizador por lado (izquierdo/derecho), pausa, cambio de lado y sugerencia de por qué lado empezar.
- **Biberón**: ml con atajos rápidos, fórmula o leche materna.
- **Sólidos**: alimento, "primera vez" (para vigilar alergias) y reacción.
- **Sueño**: temporizador en vivo o registro manual; distingue siesta y sueño nocturno.
- **Pañales**: pis/caca/ambos, color con aviso si conviene consultar al pediatra.
- **Inicio**: temporizadores activos, tiempo desde la última toma/pañal, estado despierto/dormido y resumen del día.
- **Historial** agrupado por días, con filtros por tipo y borrado.
- **Editar** cualquier registro terminado y el perfil del bebé (toca su inicial en Inicio).
- **Gráficas** de 7, 14 o 30 días: tomas, ml de biberón, sueño, pañales (pis/caca) y patrón de sueño y tomas de 24 h, con medias diarias.
- **Crecimiento**: peso, talla y perímetro craneal sobre las curvas de percentiles de la OMS (P3–P97, 0-5 años) con el percentil de cada medida.
- **Hitos** (en Crecimiento → Hitos): cordón umbilical, primera sonrisa, primer diente, primera palabra (¿cuál?), primeros pasos, operaciones (¿cuál?: frenectomía, sindactilia…) y otros, en una línea de tiempo con la edad del bebé.
- **Salud**: temperatura (con aviso de fiebre), medicación con recordatorio de la próxima dosis y citas/vacunas con notificación.
- **Varios bebés** (gemelos, hermanos): toca la inicial en Inicio para cambiar, editar o añadir.
- **Notificación con cronómetro** mientras hay una toma o un sueño en marcha, con botones para cambiar de lado, pausar o terminar.
- **Widget** de pantalla de inicio: última toma, último pañal y estado de sueño, con botones de dormir/despertar y pañal.
- **Predicciones** de la próxima toma y del próximo sueño según los últimos 3 días.
- **Informe PDF** para el pediatra (desde Gráficas): medias, tabla día a día, crecimiento con percentiles, salud y próximas citas.
- **Tipo de lactancia** (materna, artificial o mixta) que oculta automáticamente los registros que no aplican, y **ocultar cualquier tipo de registro** que no uses (en el perfil del bebé).
- **Importar y exportar** (icono de flechas en Inicio):
  - Copia completa en **JSON** para cambiar de móvil o guardar una copia, y restaurarla sin duplicar.
  - Informe **PDF** para el pediatra.
  - Importar datos de **otras apps** en JSON, CSV, PDF o texto, con resumen previo de lo reconocido.
- Colores dinámicos (Material You) y modo oscuro.

## Arquitectura

- UI: Jetpack Compose + Material 3, Navigation Compose
- Estado: ViewModel + `StateFlow` (MVVM)
- Datos: Room (local, offline-first)
- Inyección de dependencias: Hilt

```
app/src/main/java/com/kbretro/babyregistry/
├── data/        # Room, repositorio, temporizador e importación/exportación (transfer/)
├── di/          # Módulos de Hilt
├── domain/      # Cálculos: resumen del día, estadísticas, curvas OMS, predicciones
├── export/      # Informe PDF
├── reminders/   # Recordatorios, notificación del temporizador y reprogramación al reiniciar
├── widget/      # Widget de pantalla de inicio (Glance)
└── ui/          # Pantallas: home, history, stats, growth, health, entry, onboarding
```

## Compilar

Requisitos: Android Studio (Ladybug o superior) con JDK 17.

```bash
./gradlew assembleDebug        # APK de prueba en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # tests unitarios
```

Cada push compila la app en GitHub Actions; el APK de prueba se puede descargar
desde la pestaña **Actions → (ejecución) → Artifacts**.

## Hoja de ruta

- ~~**Fase 2**: gráficas, crecimiento con percentiles OMS, salud y edición de registros.~~ ✅
- ~~**Fase 3**: widgets, notificación con temporizador, predicciones, varios bebés, exportar PDF.~~ ✅
- **Sincronización entre cuidadores** (Firebase): pendiente de crear el proyecto de Firebase.

## Datos de crecimiento

Las tablas LMS de `domain/WhoTables.kt` se generan a partir de los
[Patrones de Crecimiento Infantil de la OMS](https://www.who.int/tools/child-growth-standards)
(vía el paquete `pygrowup`, licencia BSD). Son orientativas y no sustituyen el criterio del pediatra.

## Firma

`app/debug.keystore` es una clave de **depuración** con la contraseña estándar de Android (`android`).
Se guarda en el repositorio a propósito para que todos los APK de CI tengan la misma firma y se puedan
actualizar sin desinstalar. No es un secreto y no sirve para publicar: para Google Play se usará una
clave de subida aparte, guardada en los *secrets* de GitHub.
