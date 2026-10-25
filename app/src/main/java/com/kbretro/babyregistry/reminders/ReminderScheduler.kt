package com.kbretro.babyregistry.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Programa recordatorios con AlarmManager. Cada recordatorio se identifica por su URI. */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(key: String, at: Long, title: String, text: String) {
        if (at <= System.currentTimeMillis()) return
        val intent = intent(key)
            .putExtra(ReminderReceiver.EXTRA_TITLE, title)
            .putExtra(ReminderReceiver.EXTRA_TEXT, text)
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Alarma inexacta: no requiere el permiso de alarmas exactas y se retrasa como mucho unos minutos.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    fun cancel(key: String) {
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent(key),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun intent(key: String) = Intent(context, ReminderReceiver::class.java)
        .setData(Uri.parse("babyregistry://reminder/$key"))

    companion object {
        const val CHANNEL_ID = "reminders"

        fun eventKey(id: Long) = "event/$id"
        fun appointmentKey(id: Long) = "appointment/$id"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Recordatorios", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Próximas dosis de medicación y citas"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
