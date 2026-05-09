package com.kptgames.vocabuildary.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kptgames.vocabuildary.MainActivity
import com.kptgames.vocabuildary.R
import com.kptgames.vocabuildary.data.ApiFactory
import com.kptgames.vocabuildary.data.AppPreferences
import com.kptgames.vocabuildary.data.MobileNotificationItem
import com.kptgames.vocabuildary.data.GatewayAuthManager
import com.kptgames.vocabuildary.data.ReminderSlot
import com.kptgames.vocabuildary.data.VocabuildaryRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object NativeNotificationHelper {
    private const val CHANNEL_ID = "vocabuildary_daily"
    private const val CHANNEL_NAME = "Daily vocabulary"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily Vocabuildary native reminders"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, item: MobileNotificationItem) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val allowed = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!allowed) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(item.title.ifBlank { "Vocabuildary" })
            .setContentText(item.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(item.id, notification)
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        MobileNotificationWorker.enqueue(context)
        runBlocking {
            ReminderScheduler.scheduleNextFromStored(context)
        }
    }
}

class MobileNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        val authManager = GatewayAuthManager(preferences)
        authManager.load()
        if (!authManager.isAuthorized()) return Result.success()

        val (api, client) = ApiFactory.create(authManager)
        val repository = VocabuildaryRepository(api, client, preferences)
        return try {
            val notifications = repository.syncDueMobileNotifications()
            notifications.forEach { item ->
                NativeNotificationHelper.show(applicationContext, item)
                repository.markMobileNotificationDelivered(item.id)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "vocabuildary-mobile-notification-sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MobileNotificationWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

object ReminderScheduler {
    private const val REQUEST_CODE = 8721
    private val gson = Gson()

    suspend fun saveAndSchedule(context: Context, slots: List<ReminderSlot>) {
        val preferences = AppPreferences(context)
        preferences.saveReminderSlotsJson(gson.toJson(slots))
        scheduleNext(context, slots)
    }

    suspend fun scheduleNextFromStored(context: Context) {
        val preferences = AppPreferences(context)
        val json = preferences.getReminderSlotsJson() ?: return
        val type = object : TypeToken<List<ReminderSlot>>() {}.type
        val slots = runCatching { gson.fromJson<List<ReminderSlot>>(json, type) }.getOrNull()
        if (!slots.isNullOrEmpty()) scheduleNext(context, slots)
    }

    fun scheduleNext(context: Context, slots: List<ReminderSlot>) {
        val next = nextAlarmTime(slots) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            pendingIntent
        )
    }

    private fun nextAlarmTime(slots: List<ReminderSlot>): ZonedDateTime? {
        val now = ZonedDateTime.now()
        return slots
            .filter { it.enabled }
            .mapNotNull { slot ->
                val zone = runCatching { ZoneId.of(slot.timezone) }.getOrDefault(ZoneId.systemDefault())
                val time = runCatching { LocalTime.parse(slot.timeOfDay) }.getOrNull() ?: return@mapNotNull null
                var candidate = ZonedDateTime.of(LocalDate.now(zone), time, zone)
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
                candidate.withZoneSameInstant(ZoneId.systemDefault())
            }
            .minByOrNull { it.toInstant() }
    }
}
