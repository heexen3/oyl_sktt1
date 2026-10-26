package cl.oyl.frutossecos.data.repository.local

import cl.oyl.frutossecos.core.notifications.SystemNotifier
import cl.oyl.frutossecos.data.local.OylDatabase
import cl.oyl.frutossecos.data.local.entity.NotificationEntity
import cl.oyl.frutossecos.domain.model.NotificationType
import cl.oyl.frutossecos.domain.model.Role
import javax.inject.Inject
import javax.inject.Singleton

/** Crea notificaciones en Room (bandeja) y, si el destinatario es la sesión actual, una notificación del sistema. */
@Singleton
class NotificationPublisher @Inject constructor(
    private val db: OylDatabase,
    private val systemNotifier: SystemNotifier,
) {
    data class Pending(val userId: Long, val entity: NotificationEntity)

    suspend fun build(userId: Long, type: NotificationType, title: String, message: String, route: String?, targetId: Long?) =
        Pending(userId, NotificationEntity(userId = userId, type = type, title = title, message = message,
            createdAt = System.currentTimeMillis(), targetRoute = route, targetId = targetId))

    suspend fun toAdmins(type: NotificationType, title: String, message: String, route: String?, targetId: Long?): List<Pending> =
        db.userDao().idsByRole(Role.ADMINISTRADOR).map { build(it, type, title, message, route, targetId) }

    /** Inserta (dentro o fuera de una transacción) y devuelve los ids generados. */
    suspend fun insert(pending: List<Pending>): List<Pair<Long, Pending>> =
        pending.map { db.notificationDao().insert(it.entity) to it }

    /** Avisa por la bandeja del sistema solo al usuario con sesión activa y notificaciones habilitadas. */
    suspend fun alertIfCurrent(inserted: List<Pair<Long, Pending>>) {
        val session = db.sessionDao().get() ?: return
        val user = db.userDao().getById(session.userId) ?: return
        if (!user.notificationsEnabled) return
        inserted.filter { it.second.userId == session.userId }.forEach { (id, p) ->
            val route = p.entity.targetRoute?.let { r -> p.entity.targetId?.let { "$r?id=$it" } ?: r }
            systemNotifier.show(id.toInt(), p.entity.title, p.entity.message, route)
        }
    }
}
