package tech.hyperjump.hypertrace.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tech.hyperjump.hypertrace.BluetoothServiceUtil
import tech.hyperjump.hypertrace.logging.CentralLog

class StartOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            CentralLog.d("StartOnBootReceiver", "boot completed received")

            try {
                //can i try a scheduled service start here?
                CentralLog.d("StartOnBootReceiver", "Attempting to start service")
                BluetoothServiceUtil.scheduleStartMonitoringService(context, 500)
            } catch (e: Throwable) {
                CentralLog.e("StartOnBootReceiver", e.localizedMessage)
                e.printStackTrace()
            }

        }
    }
}
