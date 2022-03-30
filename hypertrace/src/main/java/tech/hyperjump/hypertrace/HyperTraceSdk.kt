package tech.hyperjump.hypertrace

import android.Manifest
import android.app.Notification
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationChannelCompat
import io.bluetrace.opentrace.BluetoothServiceUtil
import io.bluetrace.opentrace.idmanager.TempIDManager
import io.bluetrace.opentrace.logging.CentralLog
import io.bluetrace.opentrace.services.BluetoothMonitoringService
import io.bluetrace.opentrace.streetpass.CentralDevice
import io.bluetrace.opentrace.streetpass.PeripheralDevice
import io.bluetrace.opentrace.streetpass.persistence.StreetPassRecordDatabase
import io.bluetrace.opentrace.streetpass.uploader.TraceUploader
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import pub.devrel.easypermissions.EasyPermissions
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

    @VisibleForTesting
    fun setConfig(config: Config) {
        CONFIG = config
    }

    fun startService(config: Config) {
        validatePermissions()
        checkBleSupport()
        config.validateConfig()
        CONFIG = config
        BluetoothServiceUtil.startBluetoothMonitoringService(appContext)
    }

    fun stopService() {
        BluetoothServiceUtil.stopBluetoothMonitoringService(appContext)
    }

    suspend fun getHandshakePin(): String {
        return TraceUploader.getHandshakePin()
    }

    suspend fun uploadEncounterRecords(secret: String) {
        val encodedUriSecret = URLEncoder.encode(secret, "UTF-8")
        TraceUploader.uploadEncounterRecords(encodedUriSecret)
    }

    suspend fun countEncounters(before: Long = CONFIG.recordTTL): Int {
        return StreetPassRecordDatabase.getDatabase(appContext)
                .recordDao()
                .countRecords(before)
    }

    suspend fun removeEncounters(before: Long = CONFIG.recordTTL) {
        return StreetPassRecordDatabase.getDatabase(appContext)
                .recordDao()
                .purgeOldRecords(before)
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
            val advertising: Advertising = Advertising.Enable(
                    duration = 180_000, // 30 minutes
                    interval = 6_000,
            ),
            val purgeRecordInterval: Long = 86_400_000, // 24 hours
            val recordTTL: Long = 1_814_400_000, // 21 days
            val maxPeripheralQueueTime: Long = 10_000,
            val temporaryIdCheckInterval: Long = 600_000, // 10 minutes
            val bluetoothServiceHeartBeat: Long = 900_000, // 15 minutes
            val deviceConnectionTimeout: Long = 6_000,
            val deviceBlacklistDuration: Long = 90_000,
            val certificatePinner: CertificatePinner? = null,
            val okHttpConfig: (OkHttpClient.Builder.() -> Unit)? = null
    ) {

        sealed class Advertising {

            @Keep
            object Disable : Advertising()

            @Keep
            class Enable(val duration: Long, val interval: Long) : Advertising()
        }

        fun validateConfig() {
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
