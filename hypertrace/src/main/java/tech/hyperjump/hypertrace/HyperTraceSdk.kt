package tech.hyperjump.hypertrace

import android.Manifest
import android.app.Notification
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.NotificationChannelCompat
import okhttp3.CertificatePinner
import pub.devrel.easypermissions.EasyPermissions
import tech.hyperjump.hypertrace.idmanager.TempIDManager
import tech.hyperjump.hypertrace.logging.CentralLog
import tech.hyperjump.hypertrace.services.BluetoothMonitoringService
import tech.hyperjump.hypertrace.streetpass.CentralDevice
import tech.hyperjump.hypertrace.streetpass.PeripheralDevice
import tech.hyperjump.hypertrace.streetpass.uploader.TraceUploader
import java.net.URLEncoder
import java.util.*

@Keep
object HyperTraceSdk {

    internal lateinit var appContext: Context
    internal val ORG
        get() = CONFIG.organization
    internal lateinit var CONFIG: Config
        private set

    private const val TAG = "HyperTraceSDK"

    fun startService(config: Config) {
        validatePermissions()
        checkBleSupport()
        config.validateConfig()
        CONFIG = config
        BluetoothServiceUtil.startBluetoothMonitoringService(appContext)
    }

    suspend fun getHandshakePin(): String? {
        return TraceUploader.getHandshakePin()
    }

    suspend fun uploadEncounterRecords(secret: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val encodedUriSecret = URLEncoder.encode(secret, "UTF-8")
        TraceUploader.uploadEncounterRecords(encodedUriSecret, onSuccess, onError)
    }

    internal fun thisDeviceMsg(): String {
        BluetoothMonitoringService.broadcastMessage?.let {
            CentralLog.i(TAG, "Retrieved BM for storage: $it")

            if (!it.isValidForCurrentTime()) {

                val fetch = TempIDManager.retrieveTemporaryID(appContext)
                fetch?.let {
                    CentralLog.i(TAG, "Grab New Temp ID")
                    BluetoothMonitoringService.broadcastMessage = it
                }

                if (fetch == null) {
                    CentralLog.e(TAG, "Failed to grab new Temp ID")
                }

            }
        }
        return BluetoothMonitoringService.broadcastMessage?.tempID ?: "Missing TempID"
    }

    internal fun asPeripheralDevice(): PeripheralDevice {
        return PeripheralDevice(Build.MODEL, "SELF")
    }

    internal fun asCentralDevice(): CentralDevice {
        return CentralDevice(Build.MODEL, "SELF")
    }

    private fun validatePermissions() {
        val permissionCheck = EasyPermissions.hasPermissions(appContext, Manifest.permission.BLUETOOTH)
        if (!permissionCheck) throw Exception("Requires bluetooth and location permissions.")
    }

    private fun checkBleSupport() {
        val btManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter?.isEnabled != true) {
            throw Exception("Requires bluetooth to be enabled.")
        }
        if (adapter.bluetoothLeScanner == null) {
            throw Exception("This device does not support bluetooth low energy.")
        }
    }

    @Keep
    data class Config(
            val notificationChannelCreator: () -> NotificationChannelCompat,
            val foregroundNotificationCreator: (context: Context) -> Notification,
            val bluetoothFailedNotificationCreator: (context: Context) -> Notification,
            val userId: String,
            val organization: String,
            val baseUrl: String,
            val bleServiceUuid: String,
            val bleCharacteristicUuid: String,
            val debug: Boolean = false,
            val keepAliveService: Boolean = false,
            val scanDuration: Long = 10_000,
            val minScanInterval: Long = 30_000,
            val maxScanInterval: Long = 40_000,
            val advertisingDuration: Long = 180_000, // 30 minutes
            val advertisingInterval: Long = 6_000,
            val purgeRecordInterval: Long = 86_400_000, // 24 hours
            val recordTTL: Long = 1_814_400_000, // 21 days
            val maxPeripheralQueueTime: Long = 10_000,
            val temporaryIdCheckInterval: Long = 600_000, // 10 minutes
            val bluetoothServiceHeartBeat: Long = 900_000, // 15 minutes
            val deviceConnectionTimeout: Long = 6_000,
            val deviceBlacklistDuration: Long = 90_000,
            val certificatePinner: CertificatePinner? = null
    ) {

        fun validateConfig() {
            if (userId.length < 21) throw Exception("User ID must have exactly 21 characters.")
            if (organization.isEmpty()) throw Exception("Organization Code cannot be empty.")
            if (baseUrl.isEmpty()) throw Exception("Base URL cannot be empty.")
            if (baseUrl.last() != '/') throw Exception("Base URL must end with slash '/'.")
            try {
                UUID.fromString(bleServiceUuid)
                UUID.fromString(bleCharacteristicUuid)
            } catch (e: IllegalArgumentException) {
                throw Exception("BLE Service UUID and BLE Characteristic UUID must be valid UUID.")
            }
        }
    }
}
