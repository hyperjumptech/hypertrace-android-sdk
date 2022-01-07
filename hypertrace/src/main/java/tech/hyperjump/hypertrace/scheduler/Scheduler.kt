package tech.hyperjump.hypertrace.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import tech.hyperjump.hypertrace.Preference
import tech.hyperjump.hypertrace.logging.CentralLog

internal object Scheduler {
    const val TAG = "Scheduler"

    fun scheduleServiceIntent(
            requestCode: Int,
            context: Context,
            intent: Intent,
            timeFromNowInMillis: Long
    ) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val alarmIntent = PendingIntent.getService(context, requestCode, intent, flags)

        alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + timeFromNowInMillis, alarmIntent
        )
    }

    fun scheduleRepeatingServiceIntent(
            requestCode: Int,
            context: Context,
            intent: Intent,
            intervalMillis: Long
    ) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val alarmIntent = PendingIntent.getService(context, requestCode, intent, flags)

        CentralLog.d(
                TAG,
                "Purging alarm set to ${Preference.getLastPurgeTime(context) + intervalMillis}"
        )
        alarmMgr.setRepeating(
                AlarmManager.RTC,
                Preference.getLastPurgeTime(context) + intervalMillis,
                intervalMillis,
                alarmIntent
        )
    }

    fun cancelServiceIntent(requestCode: Int, context: Context, intent: Intent) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val alarmIntent = PendingIntent.getService(context, requestCode, intent, flags)
        alarmIntent.cancel()
    }
}
