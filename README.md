# Baby Registry 👶

App Android para registrar el día a día del bebé: tomas, sueño, pañales y más.
Hecha en **Kotlin + Jetpack Compose** (Material 3), funciona sin conexión.

## Funciones (fase 1)

- **Pecho** con temporizador por lado (izquierdo/derecho), pausa, cambio de lado y sugerencia de por qué lado empezar.
- **Biberón**: ml con atajos rápidos, fórmula o leche materna.
- **Sólidos**: alimento, "primera vez" (para vigilar alergias) y reacción.
- **Sueño**: temporizador en vivo o registro manual; distingue siesta y sueño nocturno.
- **Pañales**: pis/caca/ambos, color con aviso si conviene consultar al pediatra.
- **Inicio**: temporizadores activos, tiempo desde la última toma/pañal, estado despierto/dormido y resumen del día.
- **Historial** agrupado por días, con filtros por tipo y borrado.
- Colores dinámicos (Material You) y modo oscuro.

## Arquitectura

- UI: Jetpack Compose + Material 3, Navigation Compose
- Estado: ViewModel + `StateFlow` (MVVM)
- Datos: Room (local, offline-first)
- Inyección de dependencias: Hilt

```
app/src/main/java/com/kbretro/babyregistry/
├── data/        # Room: entidades, DAOs, repositorio, lógica del temporizador
├── di/          # Módulos de Hilt
├── domain/      # Cálculos (resumen del día)
└── ui/          # Pantallas: home, history, onboarding, theme, common
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

- **Fase 2**: gráficas y tendencias, crecimiento con percentiles OMS, salud (temperatura, medicación, vacunas), editar registros.
- **Fase 3**: sincronización entre cuidadores (Firebase), widgets, notificación con temporizador, predicciones de próxima toma/siesta, varios bebés, exportar PDF.
