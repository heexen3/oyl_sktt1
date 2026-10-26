# OYL Frutos Secos — App Android

App Android (Kotlin + Jetpack Compose + Material 3) para un emprendimiento de frutos secos, semillas y deshidratados.
Datos 100 % ficticios.

## Estado

| Fase | Estado |
|---|---|
| 0 — Proyecto, dependencias, Git, README | ✅ |
| 1 — Tema M3, navegación (15 rutas), Room + seed, Hilt, pruebas DAO | ✅ |
| 2 — Bienvenida, registro, inicio de sesión (PBKDF2, sesión persistente por rol) | ✅ |
| 3 — Dashboard por rol, catálogo (búsqueda + categorías) y detalle | ✅ |
| 4 — Carrito por usuario y confirmación de pedido (validación de stock, retiro/despacho) | ✅ |
| 5 — Escáner QR (CameraX + ML Kit, permiso de cámara, ingreso manual) | ✅ |
| 6 — Gestión de productos (CRUD, foto desde galería, activar/desactivar) e inventario (entradas/salidas) | ✅ |
| 7 — Pedidos (filtros, detalle, cambio de estado, cancelación) y notificaciones (lista + aviso local del sistema) | ✅ (FCM pendiente) |
| 8 — Mapa osmdroid (tiendas, "Cómo llegar", ubicación opcional) y perfil (edición, avatar, permisos, cerrar sesión) | ✅ |
| 9 — Backend Spring Boot (Kotlin + H2) | ✅ (opcional; la app no lo usa en esta versión — ver `backend/README.md`) |
| 10 — Integración con backend (repositorios `Remote*`) y pulido | ⏳ parcial (lint sin errores; integración remota pendiente) |
| 11 — APK release firmado (R8 + shrinkResources) | ✅ |

## Decisiones de esta versión

- **Mapa:** osmdroid (OpenStreetMap, sin API key). Se reemplazó Maps Compose / Play Services Maps por `org.osmdroid:osmdroid-android`.
- **Autenticación:** JWT (el `AuthInterceptor` envía `Authorization: Bearer <jwt>` cuando exista backend).
- **Datos:** en esta versión la app funciona **solo con SQLite local (Room)**. El flag `BuildConfig.USE_REMOTE_REPOSITORIES` vale `false` y Hilt inyecta las implementaciones `Local*`. Las interfaces de repositorio quedan listas para agregar las `Remote*`.
- **Backend:** Spring Boot 4.1 en Kotlin con H2 para desarrollo/pruebas (carpeta `backend/`, ver `backend/README.md`). La app aún no lo consume.
- **Firebase / FCM:** la dependencia `firebase-messaging` está incluida, pero el plugin `com.google.gms.google-services` **no se aplica** porque requiere un `google-services.json` real (que nunca se versiona). Mientras tanto, los avisos (nuevo pedido, cambio de estado, stock bajo) se guardan en Room por usuario y se muestran como notificación local del sistema (canal propio, requiere `POST_NOTIFICATIONS` en Android 13+); al tocarla abre la pantalla relacionada.
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
backend/   (Spring Boot + H2, opcional)
```

## Base de datos local (Room)

Tablas: `users`, `session`, `categories`, `products`, `cart_items`, `orders`, `order_items`, `notifications`, `stores`,
`pending_stock_movements`. Toda tabla con datos personales lleva `userId` y sus consultas filtran por `userId`
(varios clientes en el mismo teléfono no mezclan carrito, pedidos ni avisos). El esquema se exporta en `app/schemas/`.

## Pasos manuales / secretos (fuera del repositorio)

### Firebase Cloud Messaging (pendiente)
1. Crear el proyecto en la consola de Firebase con el paquete `cl.oyl.frutossecos` y descargar `google-services.json` en `app/` (está en `.gitignore`).
2. Agregar el plugin `com.google.gms.google-services` en `gradle/libs.versions.toml` y en `app/build.gradle.kts`.
3. Implementar un `FirebaseMessagingService` que reenvíe los mensajes a `SystemNotifier` y registre el token en `/devices/fcm-token` cuando exista backend.

### Firma del APK release
La keystore **nunca** se versiona (`*.jks` y `keystore.properties` están en `.gitignore`). `app/build.gradle.kts` lee la firma desde
`keystore.properties` en la raíz del proyecto:
```properties
storeFile=/ruta/absoluta/oyl-release.jks
storePassword=...
keyAlias=oyl
keyPassword=...
```
o desde las variables de entorno `OYL_KEYSTORE_FILE`, `OYL_KEYSTORE_PASSWORD`, `OYL_KEY_ALIAS`, `OYL_KEY_PASSWORD`. Luego:
```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```
Si no hay datos de firma, el build release se genera sin firmar. Para crear una keystore nueva:
`keytool -genkeypair -v -keystore oyl-release.jks -alias oyl -keyalg RSA -keysize 2048 -validity 10000`.

## Funcionamiento sin backend (esta versión)

- El stock se descuenta en una transacción Room al confirmar el pedido; si algún producto no alcanza, el pedido no se crea y el carrito marca el producto con problema. Cancelar un pedido (solo en *Pendiente de validación* o *Confirmado*) devuelve el stock.
- Estados: Pendiente de validación → Confirmado → Preparado → Entregado (o Cancelado). Despacho: $2.500, gratis desde $25.000; retiro en tienda sin costo.
- Los ajustes de inventario se aplican al stock local y quedan además en `pending_stock_movements` (estado PENDIENTE, con `localId` UUID) para sincronizarlos con el backend cuando exista.
- Códigos QR de los productos de prueba: `OYL-P-0001`, `OYL-P-0002`, … (se pueden escribir a mano en el escáner).
- La foto de producto se elige desde la galería (Photo Picker) y se copia al almacenamiento interno de la app.

## Desviaciones respecto del plan original

- Mapa con osmdroid en lugar de Google Maps (decisión del cliente).
- Sin GitHub ni Trello: Git local (`main`, `develop`, `feature/*`) y entrega en `.zip`.
- Sin backend ni FCM en esta versión: repositorios `Local*` y notificaciones locales del sistema.
- El filtro "Todos" de categorías es un chip de la interfaz (no una fila de la tabla `categories`).
- Pruebas con Robolectric (Room en memoria) además de JUnit; se agregó `material-icons-extended`.
- El APK release pesa ~23 MB porque ML Kit incluye el modelo de lectura de códigos dentro del APK (funciona sin Google Play Services).
