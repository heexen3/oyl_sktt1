# OYL Backend (Fase 9) — Spring Boot 4.1 + Kotlin + H2

Backend REST opcional. **La app Android de esta versión NO lo usa** (datos solo en SQLite/Room local, decisión del cliente). Queda listo para la integración futura (repositorios `Remote*`, Fase 10).

## Ejecutar
```bash
cd backend
export JAVA_HOME=/ruta/a/jdk-21
./gradlew bootRun          # http://localhost:8080  (desde el emulador: http://10.0.2.2:8080)
./gradlew test             # 7 pruebas (integración MockMvc + concurrencia)
```
- Base de datos: H2 en memoria (`jdbc:h2:mem:oyl`), consola en `/h2-console`. Solo desarrollo/pruebas.
- Datos semilla iguales a la app: admin demo, 4 categorías, 3 tiendas, 11 productos (QR `OYL-P-0001`…).
- Variables de entorno: `OYL_JWT_SECRET` (≥32 bytes, **obligatorio cambiar fuera de dev**), `OYL_ADMIN_PASSWORD` (clave del admin demo).

## Endpoints (`/api/v1`)
| Método | Ruta | Acceso |
|---|---|---|
| POST | `/auth/register`, `/auth/login` (alias o email + clave + rol) | público |
| GET | `/products?q=&categoryId=`, `/products/{id}`, `/products/by-qr/{code}`, `/stores` | público |
| POST/PUT/PATCH | `/products`, `/products/{id}`, `/products/{id}/active`, `/products/{id}/image` (multipart) | ADMINISTRADOR |
| POST | `/cart/validate`, `/orders` | autenticado |
| GET | `/orders`, `/orders/{id}` (cliente: solo los suyos; admin: todos) | autenticado |
| PATCH | `/orders/{id}/status` | ADMINISTRADOR |
| GET/POST | `/inventory?lowStock=`, `/inventory/movements` (idempotente por `localId`) | ADMINISTRADOR |
| GET/POST | `/notifications`, `/notifications/{id}/read`, `/devices/fcm-token` | autenticado |

Errores JSON `{code, message, details}`; stock insuficiente → **409 `STOCK_INSUFICIENTE`** con detalle por producto.

## Diseño
- JWT HS256 (Spring Security OAuth2 Resource Server + Nimbus), sesión stateless, BCrypt, claim `role` → `ROLE_*`.
- Pedido transaccional con bloqueo pesimista (`SELECT … FOR UPDATE` ordenado por id) → sin sobreventa (probado con 8 pedidos simultáneos por la última unidad).
- Cancelar un pedido repone stock; transiciones de estado validadas.
- Push: `LoggingPushSender` simula FCM (solo log). **Pendiente:** Firebase Admin SDK con credenciales de servicio (paso manual, fuera del repo).
- Sin Swagger/OpenAPI en esta versión.
