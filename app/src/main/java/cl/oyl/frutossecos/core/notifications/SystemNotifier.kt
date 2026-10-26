package cl.oyl.frutossecos.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.oyl.frutossecos.MainActivity
import cl.oyl.frutossecos.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notificaciones del sistema. En esta versión (solo local) se usan como respaldo local de las push;
 * FCM se conectará en la fase del backend (ver README).
 */
interface SystemNotifier {
    fun show(id: Int, title: String, message: String, targetRoute: String?)
}

@Singleton
class AndroidSystemNotifier @Inject constructor(@ApplicationContext private val context: Context) : SystemNotifier {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun show(id: Int, title: String, message: String, targetRoute: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, targetRoute)
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_receipt)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) { /* permiso revocado */ }
    }

    companion object {
        const val CHANNEL_ID = "oyl_pedidos"
        const val EXTRA_ROUTE = "target_route"
    }
}
