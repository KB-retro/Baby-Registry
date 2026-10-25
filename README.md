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
- **Salud**: temperatura (con aviso de fiebre), medicación con recordatorio de la próxima dosis y citas/vacunas con notificación.
- Colores dinámicos (Material You) y modo oscuro.

## Arquitectura

- UI: Jetpack Compose + Material 3, Navigation Compose
- Estado: ViewModel + `StateFlow` (MVVM)
- Datos: Room (local, offline-first)
- Inyección de dependencias: Hilt

```
app/src/main/java/com/kbretro/babyregistry/
├── data/        # Room: entidades, DAOs, migraciones, repositorio, temporizador
├── di/          # Módulos de Hilt
├── domain/      # Cálculos: resumen del día, estadísticas, curvas OMS
├── reminders/   # Notificaciones (AlarmManager) y reprogramación al reiniciar
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
- **Fase 3**: sincronización entre cuidadores (Firebase), widgets, notificación con temporizador, predicciones de próxima toma/siesta, varios bebés, exportar PDF.

## Datos de crecimiento

Las tablas LMS de `domain/WhoTables.kt` se generan a partir de los
[Patrones de Crecimiento Infantil de la OMS](https://www.who.int/tools/child-growth-standards)
(vía el paquete `pygrowup`, licencia BSD). Son orientativas y no sustituyen el criterio del pediatra.
