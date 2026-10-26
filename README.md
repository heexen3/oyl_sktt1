# OYL Frutos Secos — App Android

App Android (Kotlin + Jetpack Compose + Material 3) para un emprendimiento de frutos secos, semillas y deshidratados.
Datos 100 % ficticios.

## Estado

| Fase | Estado |
|---|---|
| 0 — Proyecto, dependencias, Git, README | ✅ |
| 1 — Tema M3, navegación (15 rutas), Room + seed, Hilt, pruebas DAO | ✅ |

## Decisiones de esta versión

- **Mapa:** osmdroid (OpenStreetMap, sin API key). Se reemplazó Maps Compose / Play Services Maps por `org.osmdroid:osmdroid-android`.
- **Autenticación:** JWT (el `AuthInterceptor` envía `Authorization: Bearer <jwt>` cuando exista backend).
- **Datos:** en esta versión la app funciona **solo con SQLite local (Room)**. El flag `BuildConfig.USE_REMOTE_REPOSITORIES` vale `false` y Hilt inyecta las implementaciones `Local*`. Las interfaces de repositorio quedan listas para agregar las `Remote*`.
- **Backend:** Spring Boot en Kotlin con H2 para desarrollo/pruebas — pendiente (carpeta `backend/`).
- **Firebase / FCM:** la dependencia `firebase-messaging` está incluida, pero el plugin `com.google.gms.google-services` **no se aplica** porque requiere un `google-services.json` real (que nunca se versiona). Se conectará en la Fase 7: copiar `google-services.json` a `app/` y agregar el plugin.
- **Repositorio:** Git local con ramas `main`, `develop` y `feature/*` (sin GitHub ni Trello por ahora).

## Requisitos

- JDK 17 o superior (probado con OpenJDK 21).
- Android SDK con `platforms;android-37.0` y `build-tools;37.0.0` (o Android Studio reciente con AGP 9.4).
- Gradle 9.8.2 (incluido vía wrapper).

## Cómo ejecutar

1. Abrir la carpeta en Android Studio (o crear `local.properties` con `sdk.dir=/ruta/al/Android/sdk`).
2. Compilar e instalar en un emulador/dispositivo (Android 8.0+, minSdk 26):
   ```bash
   ./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
   ./gradlew installDebug
   ```
3. Pruebas unitarias (incluye pruebas Room en memoria con Robolectric):
   ```bash
   ./gradlew testDebugUnitTest
   ```

## Credenciales de demostración

La cuenta de Administrador se siembra en el primer arranque (`DatabaseSeeder`):

| Campo | Valor |
|---|---|
| Nombre | Administrador |
| Alias | `admin` |
| Correo | `admin@admin.cl` |
| Teléfono | +56 9 1234 1234 |
| Dirección | Santiago |
| Contraseña demo | `OylAdmin2026!` (constante `SeedData.DEMO_ADMIN_PASSWORD`) |

La contraseña se guarda como hash **PBKDF2-HMAC-SHA256 (120.000 iteraciones) con sal aleatoria**; nunca en texto plano.
Los clientes se registran desde la pantalla "Crear cuenta".

## Estructura

```
app/src/main/java/cl/oyl/frutossecos/
├── core/ (di, navigation, ui/theme, ui/components, util, permissions)
├── data/ (local: Room entidades/DAOs/seed · remote: Retrofit · repository)
├── domain/model/
└── feature/
backend/   (pendiente)
```

## Base de datos local (Room)

Tablas: `users`, `session`, `categories`, `products`, `cart_items`, `orders`, `order_items`, `notifications`, `stores`,
`pending_stock_movements`. Toda tabla con datos personales lleva `userId` y sus consultas filtran por `userId`
(varios clientes en el mismo teléfono no mezclan carrito, pedidos ni avisos). El esquema se exporta en `app/schemas/`.

## Pasos manuales / secretos (fuera del repositorio)

- `google-services.json` (Firebase) — Fase 7.
- Keystore de firma release y sus contraseñas — Fase 11 (`keystore.properties`, no versionado).
