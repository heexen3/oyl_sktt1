package cl.oyl.frutossecos.domain.model

/** Rol del usuario. */
enum class Role { CLIENTE, ADMINISTRADOR }

/** Estados de un pedido (se guarda el nombre del enum en Room). */
enum class OrderStatus { PENDIENTE_VALIDACION, CONFIRMADO, PREPARADO, ENTREGADO, CANCELADO }

/** Modalidad de entrega del pedido. */
enum class DeliveryMode { RETIRO, DESPACHO }

/** Tipo de punto en el mapa. */
enum class StoreType { TIENDA, RETIRO }

/** Tipo de movimiento de inventario. */
enum class MovementType { ENTRADA, SALIDA }

/** Estado de sincronización de un movimiento local con el backend. */
enum class SyncStatus { PENDIENTE, ENVIANDO, SINCRONIZADO, ERROR }

/** Tipo de aviso para el usuario. */
enum class NotificationType { NUEVO_PEDIDO, CAMBIO_ESTADO, STOCK_BAJO, GENERAL }
