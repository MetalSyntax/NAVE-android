# NAVE Motos — Android (Kotlin + Jetpack Compose + Material Design 3 + Clean Architecture)

Aplicación Android de gestión de motocicletas, telemetría de combustible, rutas y mantenimiento preventivo basada en [MetalSyntax/NAVE](https://github.com/MetalSyntax/NAVE), implementada en **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, **DataStore** y **Hilt** bajo **Clean Architecture**.

## Arquitectura Clean (`com.nave.moto`)

1. **Domain (`domain/`)**: Modelos puros (`Motorcycle`, `FuelLog`, `MaintenanceRecord`, `ServiceSchedule`, `ServiceAlert`, `RouteRecord`, `MotoManual`), interfaz `MotoRepository` y casos de uso (`ObserveGarageDashboardUseCase`, `LogFuelRefillUseCase`, `RecordMaintenanceUseCase`, `FinishRouteUseCase`, `ComputeServiceAlertsUseCase`, `ComputeTelemetryStatsUseCase`).
2. **Data (`data/`)**: Base de datos local Room (`MotoDatabase`, `@Entity`, `@Dao`), mappers bidireccionales (`MotoMappers.kt`), preferencias DataStore e implementación `MotoRepositoryImpl`.
3. **DI (`di/`)**: `AppModule.kt` con Hilt `@Singleton`.
4. **Presentation (`presentation/`)**: Paleta NAVE MD3 (`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`), `NaveMotoViewModel` con `StateFlow` + eventos sellados (UDF), navegación MD3 (`NavigationBar` + `TopAppBar`) y pantallas Compose (`DashboardScreen`, `LogsScreen`, `MaintenanceScreen`, `GarageScreen`).
