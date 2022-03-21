package io.bluetrace.opentrace.bluetooth

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import io.bluetrace.opentrace.BluetoothServiceUtil
import io.bluetrace.opentrace.logging.CentralLog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class BLEScanner(context: Context, uuid: String, reportDelay: Long) {

    private var serviceUUID: String by Delegates.notNull()
    private var context: Context by Delegates.notNull()
    private var scanCallback: ScanCallback? = null
    private var reportDelay: Long by Delegates.notNull()
    private val scanner: BluetoothLeScanner?
        get() {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            return manager?.adapter?.bluetoothLeScanner
        }

    init {
        this.serviceUUID = uuid
        this.context = context
        this.reportDelay = reportDelay
    }

    fun startScan(scanCallback: ScanCallback) {
        val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID)))
                .build()

        val filters: ArrayList<ScanFilter> = ArrayList()
        filters.add(filter)
        val settings = ScanSettings.Builder()
                .setReportDelay(reportDelay)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()

        this.scanCallback = scanCallback
        scanner?.startScan(filters, settings, scanCallback)
    }

    fun flush() {
        scanCallback?.let {
            scanner?.flushPendingScanResults(scanCallback)
        }
    }

    fun stopScan() {
        try {
            // fixed crash if BT if turned off, stop scan will crash.
            if (scanCallback != null && BluetoothServiceUtil.isBluetoothAvailable(context)) {
                scanner?.stopScan(scanCallback)
                CentralLog.d(TAG, "scanning stopped")
            }
        } catch (e: Throwable) {
            CentralLog.e(
                    TAG,
                    "unable to stop scanning - callback null or bluetooth off? : ${e.localizedMessage}"
            )
        }
    }

    companion object {
        private const val TAG = "BLEScanner"
    }
}
