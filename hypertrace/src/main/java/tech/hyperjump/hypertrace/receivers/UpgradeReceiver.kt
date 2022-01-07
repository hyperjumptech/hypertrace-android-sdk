package tech.hyperjump.hypertrace.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tech.hyperjump.hypertrace.BluetoothServiceUtil
import tech.hyperjump.hypertrace.logging.CentralLog

class UpgradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        try {
            if (Intent.ACTION_MY_PACKAGE_REPLACED != intent!!.action) return
            // Start your service here.
            context?.let {
                CentralLog.i("UpgradeReceiver", "Starting service from upgrade receiver")
                BluetoothServiceUtil.startBluetoothMonitoringService(context)
            }
        } catch (e: Exception) {
            CentralLog.e("UpgradeReceiver", "Unable to handle upgrade: ${e.localizedMessage}")
        }
    }
}
