package tech.hyperjump.hypertrace.services

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import tech.hyperjump.hypertrace.*
import tech.hyperjump.hypertrace.bluetooth.BLEAdvertiser
import tech.hyperjump.hypertrace.bluetooth.gatt.ACTION_RECEIVED_STATUS
import tech.hyperjump.hypertrace.bluetooth.gatt.ACTION_RECEIVED_STREETPASS
import tech.hyperjump.hypertrace.bluetooth.gatt.STATUS
import tech.hyperjump.hypertrace.bluetooth.gatt.STREET_PASS
import tech.hyperjump.hypertrace.idmanager.TempIDManager
import tech.hyperjump.hypertrace.idmanager.TemporaryID
import tech.hyperjump.hypertrace.logging.CentralLog
import tech.hyperjump.hypertrace.status.Status
import tech.hyperjump.hypertrace.status.persistence.StatusRecord
import tech.hyperjump.hypertrace.status.persistence.StatusRecordStorage
import tech.hyperjump.hypertrace.streetpass.ConnectionRecord
import tech.hyperjump.hypertrace.streetpass.StreetPassScanner
import tech.hyperjump.hypertrace.streetpass.StreetPassServer
import tech.hyperjump.hypertrace.streetpass.StreetPassWorker
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecord
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecordStorage
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

class BluetoothMonitoringService : Service(), CoroutineScope {
    var worker: StreetPassWorker? = null

    private lateinit var serviceUUID: String
    private var streetPassServer: StreetPassServer? = null
    private var streetPassScanner: StreetPassScanner? = null
    private var advertiser: BLEAdvertiser? = null
    private val streetPassReceiver = StreetPassReceiver()
    private val statusReceiver = StatusReceiver()
    private val bluetoothStatusReceiver = BluetoothStatusReceiver()
    private lateinit var streetPassRecordStorage: StreetPassRecordStorage
    private lateinit var statusRecordStorage: StatusRecordStorage
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var commandHandler: CommandHandler
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var notificationShown: NotificationState? = null

    override fun onCreate() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        setup()
    }

    fun setup() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        CentralLog.setPowerManager(pm)

        commandHandler = CommandHandler(WeakReference(this))

        CentralLog.d(TAG, "Creating service - BluetoothMonitoringService")
        serviceUUID = HyperTraceSdk.CONFIG.bleServiceUuid

        worker = StreetPassWorker(this.applicationContext)

        unregisterReceivers()
        registerReceivers()

        streetPassRecordStorage = StreetPassRecordStorage(this.applicationContext)
        statusRecordStorage = StatusRecordStorage(this.applicationContext)

        setupNotifications()
        broadcastMessage = TempIDManager.retrieveTemporaryID(this.applicationContext)
    }

    fun teardown() {
        streetPassServer?.tearDown()
        streetPassServer = null

        streetPassScanner?.stopScan()
        streetPassScanner = null

        commandHandler.removeCallbacksAndMessages(null)

        BluetoothServiceUtil.cancelBMUpdateCheck(this.applicationContext)
        BluetoothServiceUtil.cancelNextScan(this.applicationContext)
        BluetoothServiceUtil.cancelNextAdvertise(this.applicationContext)
    }

    private fun setupNotifications() {
        val channel = HyperTraceSdk.CONFIG.notificationChannelCreator()
        // Set the Notification Channel for the Notification Manager.
        val mNotificationManager = NotificationManagerCompat.from(applicationContext)
        mNotificationManager.createNotificationChannel(channel)
    }

    private fun notifyLackingThings(override: Boolean = false) {
        if (notificationShown != NotificationState.LACKING_THINGS || override) {
            val notification = HyperTraceSdk.CONFIG.bluetoothFailedNotificationCreator(applicationContext)
            startForeground(NOTIFICATION_ID, notification)
            notificationShown = NotificationState.LACKING_THINGS
        }
    }

    private fun notifyRunning(override: Boolean = false) {
        if (notificationShown != NotificationState.RUNNING || override) {
            val notification = HyperTraceSdk.CONFIG.foregroundNotificationCreator(applicationContext)
            startForeground(NOTIFICATION_ID, notification)
            notificationShown = NotificationState.RUNNING
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val perms = BluetoothServiceUtil.getRequiredPermissions()
        return EasyPermissions.hasPermissions(this.applicationContext, *perms)
    }

    private fun isBluetoothEnabled(): Boolean {
        var btOn = false
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        bluetoothAdapter?.let {
            btOn = it.isEnabled
        }
        return btOn
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CentralLog.i(TAG, "Service onStartCommand")

        //check for permissions
        if (!hasLocationPermissions() || !isBluetoothEnabled()) {
            CentralLog.i(
                    TAG,
                    "location permission: ${hasLocationPermissions()} bluetooth: ${isBluetoothEnabled()}"
            )
            notifyLackingThings()
            return START_STICKY
        }

        intent?.let {
            val cmd = intent.getIntExtra(COMMAND_KEY, Command.INVALID.index)
            runService(Command.findByValue(cmd))

            return START_STICKY
        }

        if (intent == null) {
            CentralLog.e(TAG, "WTF? Nothing in intent @ onStartCommand")
//            Utils.startBluetoothMonitoringService(applicationContext)
            commandHandler.startBluetoothMonitoringService()
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_STICKY
    }

    fun runService(cmd: Command?) {

        val doWork = true
        CentralLog.i(TAG, "Command is:${cmd?.string}")

        //check for permissions
        if (!hasLocationPermissions() || !isBluetoothEnabled()) {
            CentralLog.i(
                    TAG,
                    "location permission: ${hasLocationPermissions()} bluetooth: ${isBluetoothEnabled()}"
            )
            notifyLackingThings()
            return
        }

        //show running foreground notification if its not showing that
        notifyRunning()

        when (cmd) {
            Command.ACTION_START -> {
                setupService()
                BluetoothServiceUtil.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
                BluetoothServiceUtil.scheduleRepeatingPurge(this.applicationContext, purgeInterval)
                BluetoothServiceUtil.scheduleBMUpdateCheck(this.applicationContext, bmCheckInterval)
                actionStart()
            }

            Command.ACTION_SCAN -> {
                scheduleScan()

                if (doWork) {
                    actionScan()
                }
            }

            Command.ACTION_ADVERTISE -> {
                scheduleAdvertisement()
                if (doWork) {
                    actionAdvertise()
                }
            }

            Command.ACTION_UPDATE_BM -> {
                BluetoothServiceUtil.scheduleBMUpdateCheck(this.applicationContext, bmCheckInterval)
                actionUpdateBm()
            }

            Command.ACTION_STOP -> {
                actionStop()
            }

            Command.ACTION_SELF_CHECK -> {
                BluetoothServiceUtil.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
                if (doWork) {
                    actionHealthCheck()
                }
            }

            Command.ACTION_PURGE -> {
                actionPurge()
            }

            else -> CentralLog.i(TAG, "Invalid / ignored command: $cmd. Nothing to do")
        }
    }

    private fun actionStop() {
        stopForeground(true)
        stopSelf()
        CentralLog.w(TAG, "Service Stopping")
    }

    private fun actionHealthCheck() {
        performUserLoginCheck()
        performHealthCheck()
        BluetoothServiceUtil.scheduleRepeatingPurge(this.applicationContext, purgeInterval)
    }

    private fun actionPurge() {
        performPurge()
    }

    private fun actionStart() {
        CentralLog.d(TAG, "Action Start")

        launch {
            TempIDManager.getTemporaryIDs(this@BluetoothMonitoringService.applicationContext) {
                CentralLog.d(TAG, "Get TemporaryIDs completed")
                //this will run whether it starts or fails.
                TempIDManager.retrieveTemporaryID(
                        this@BluetoothMonitoringService.applicationContext
                )?.let {
                    broadcastMessage = it
                    setupCycles()
                }
            }
        }
    }

    fun actionUpdateBm() {

        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionUpdateBM")
            //need to pull new BM
            launch {

                TempIDManager.getTemporaryIDs(this@BluetoothMonitoringService.applicationContext) {
                    //this will run whether it starts or fails.
                    val fetch = TempIDManager.retrieveTemporaryID(this@BluetoothMonitoringService.applicationContext)
                    fetch?.let {
                        CentralLog.i(TAG, "[TempID] Updated Temp ID")
                        broadcastMessage = it
                    }

                    if (fetch == null) {
                        CentralLog.e(TAG, "[TempID] Failed to fetch new Temp ID")
                    }
                }
            }
        } else {
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionUpdateBM")
        }

    }

    fun calcPhaseShift(min: Long, max: Long): Long {
        return (min + (Math.random() * (max - min))).toLong()
    }

    private fun actionScan() {
        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionScan")
            //need to pull new BM
            launch {
                TempIDManager.getTemporaryIDs(this@BluetoothMonitoringService.applicationContext) {
                    //this will run whether it starts or fails.
                    TempIDManager.retrieveTemporaryID(
                            this@BluetoothMonitoringService.applicationContext
                    )?.let {
                        broadcastMessage = it
                        performScan()
                    }
                }
            }
        } else {
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionScan")
            performScan()
        }
    }

    private fun actionAdvertise() {
        setupAdvertiser()
        if (isBluetoothEnabled()) {
            advertiser?.startAdvertising(advertisingDuration)
        } else {
            CentralLog.w(TAG, "Unable to start advertising, bluetooth is off")
        }
    }

    private fun setupService() {
        streetPassServer =
                streetPassServer ?: StreetPassServer(this.applicationContext, serviceUUID)
        setupScanner()
        setupAdvertiser()
    }

    private fun setupScanner() {
        streetPassScanner = streetPassScanner
                ?: StreetPassScanner(this, serviceUUID, scanDuration)
    }

    private fun setupAdvertiser() {
        advertiser = advertiser ?: BLEAdvertiser(this, serviceUUID)
    }

    private fun setupCycles() {
        setupScanCycles()
        setupAdvertisingCycles()
    }

    private fun setupScanCycles() {
        commandHandler.scheduleNextScan(0)
    }

    private fun setupAdvertisingCycles() {
        commandHandler.scheduleNextAdvertise(0)
    }

    private fun performScan() {
        setupScanner()
        startScan()
    }

    private fun scheduleScan() {
        if (!infiniteScanning) {
            commandHandler.scheduleNextScan(
                    scanDuration + calcPhaseShift(
                            minScanInterval,
                            maxScanInterval
                    )
            )
        }
    }

    private fun scheduleAdvertisement() {
        if (!infiniteAdvertising) {
            commandHandler.scheduleNextAdvertise(advertisingDuration + advertisingGap)
        }
    }

    private fun startScan() {

        if (isBluetoothEnabled()) {

            streetPassScanner?.let { scanner ->
                if (!scanner.isScanning()) {
                    scanner.startScan()
                } else {
                    CentralLog.e(TAG, "Already scanning!")
                }
            }
        } else {
            CentralLog.w(TAG, "Unable to start scan - bluetooth is off")
        }
    }

    private fun performUserLoginCheck() {
        // check user login
    }

    private fun performHealthCheck() {

        CentralLog.i(TAG, "Performing self diagnosis")

        if (!hasLocationPermissions() || !isBluetoothEnabled()) {
            CentralLog.i(TAG, "no location permission")
            notifyLackingThings(true)
            return
        }

        notifyRunning(true)

        //ensure our service is there
        setupService()

        if (!infiniteScanning) {
            if (!commandHandler.hasScanScheduled()) {
                CentralLog.w(TAG, "Missing Scan Schedule - rectifying")
                commandHandler.scheduleNextScan(100)
            } else {
                CentralLog.w(TAG, "Scan Schedule present")
            }
        } else {
            CentralLog.w(TAG, "Should be operating under infinite scan mode")
        }

        if (!infiniteAdvertising) {
            if (!commandHandler.hasAdvertiseScheduled()) {
                CentralLog.w(TAG, "Missing Advertise Schedule - rectifying")
//                setupAdvertisingCycles()
                commandHandler.scheduleNextAdvertise(100)
            } else {
                CentralLog.w(
                        TAG,
                        "Advertise Schedule present. Should be advertising?:  ${
                            advertiser?.shouldBeAdvertising
                                    ?: false
                        }. Is Advertising?: ${advertiser?.isAdvertising ?: false}"
                )
            }
        } else {
            CentralLog.w(TAG, "Should be operating under infinite advertise mode")
        }


    }

    private fun performPurge() {
        val context = this
        launch {
            val before = System.currentTimeMillis() - purgeTTL
            CentralLog.i(TAG, "Coroutine - Purging of data before epoch time $before")

            streetPassRecordStorage.purgeOldRecords(before)
            statusRecordStorage.purgeOldRecords(before)
            Preference.putLastPurgeTime(context, System.currentTimeMillis())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed - tearing down")
        stopService()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed")
        if (HyperTraceSdk.CONFIG.keepAliveService) BluetoothServiceUtil.startBluetoothMonitoringService(this.applicationContext)
    }

    private fun stopService() {
        teardown()
        unregisterReceivers()
        worker?.terminateConnections()
        worker?.unregisterReceivers()
        job.cancel()
    }


    private fun registerReceivers() {
        val recordAvailableFilter = IntentFilter(ACTION_RECEIVED_STREETPASS)
        localBroadcastManager.registerReceiver(streetPassReceiver, recordAvailableFilter)

        val statusReceivedFilter = IntentFilter(ACTION_RECEIVED_STATUS)
        localBroadcastManager.registerReceiver(statusReceiver, statusReceivedFilter)

        val bluetoothStatusReceivedFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStatusReceiver, bluetoothStatusReceivedFilter)

        CentralLog.i(TAG, "Receivers registered")
    }

    private fun unregisterReceivers() {
        try {
            localBroadcastManager.unregisterReceiver(streetPassReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "streetPassReceiver is not registered?")
        }

        try {
            localBroadcastManager.unregisterReceiver(statusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "statusReceiver is not registered?")
        }

        try {
            unregisterReceiver(bluetoothStatusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "bluetoothStatusReceiver is not registered?")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    inner class BluetoothStatusReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = intent.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)

                    when (state) {
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_OFF")
                            notifyLackingThings()
                            teardown()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_OFF")
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_ON")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_ON")
                            BluetoothServiceUtil.startBluetoothMonitoringService(this@BluetoothMonitoringService.applicationContext)
                        }
                    }
                }
            }
        }
    }

    inner class StreetPassReceiver : BroadcastReceiver() {

        private val TAG = "StreetPassReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STREETPASS == intent.action) {
                val connRecord = intent.getParcelableExtra<ConnectionRecord>(STREET_PASS)
                        ?: return
                CentralLog.d(TAG, "StreetPass received: $connRecord")

                if (connRecord.msg.isNotEmpty()) {
                    val record = StreetPassRecord(
                            v = connRecord.version,
                            msg = connRecord.msg,
                            org = connRecord.org,
                            modelP = connRecord.peripheral.modelP,
                            modelC = connRecord.central.modelC,
                            rssi = connRecord.rssi,
                            txPower = connRecord.txPower
                    )

                    launch {
                        CentralLog.d(
                                TAG,
                                "Coroutine - Saving StreetPassRecord: ${BluetoothServiceUtil.getDate(record.timestamp)}"
                        )
                        streetPassRecordStorage.saveRecord(record)
                    }
                }
            }
        }
    }

    inner class StatusReceiver : BroadcastReceiver() {
        private val TAG = "StatusReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STATUS == intent.action) {
                val statusRecord = intent.getParcelableExtra<Status>(STATUS)
                        ?: return
                CentralLog.d(TAG, "Status received: ${statusRecord.msg}")

                if (statusRecord.msg.isNotEmpty()) {
                    val record = StatusRecord(statusRecord.msg)
                    launch {
                        statusRecordStorage.saveRecord(record)
                    }
                }
            }
        }
    }

    enum class Command(val index: Int, val string: String) {
        INVALID(-1, "INVALID"),
        ACTION_START(0, "START"),
        ACTION_SCAN(1, "SCAN"),
        ACTION_STOP(2, "STOP"),
        ACTION_ADVERTISE(3, "ADVERTISE"),
        ACTION_SELF_CHECK(4, "SELF_CHECK"),
        ACTION_UPDATE_BM(5, "UPDATE_BM"),
        ACTION_PURGE(6, "PURGE");

        companion object {
            private val types = values().associateBy { it.index }
            fun findByValue(value: Int) = types[value]
        }
    }

    enum class NotificationState {
        RUNNING,
        LACKING_THINGS
    }

    internal companion object {

        private const val TAG = "BTMService"

        private val NOTIFICATION_ID = R.id.hypertrace_notification_id

        const val COMMAND_KEY = "${BuildConfig.LIBRARY_PACKAGE_NAME}_CMD"

        const val PENDING_START = 6
        const val PENDING_SCAN_REQ_CODE = 7
        const val PENDING_ADVERTISE_REQ_CODE = 8
        const val PENDING_HEALTH_CHECK_CODE = 9
        const val PENDING_BM_UPDATE = 11
        const val PENDING_PURGE_CODE = 12

        var broadcastMessage: TemporaryID? = null

        //should be more than advertising gap?
        val scanDuration: Long = HyperTraceSdk.CONFIG.scanDuration
        val minScanInterval: Long = HyperTraceSdk.CONFIG.minScanInterval
        val maxScanInterval: Long = HyperTraceSdk.CONFIG.maxScanInterval

        val advertisingDuration: Long = HyperTraceSdk.CONFIG.advertisingDuration
        val advertisingGap: Long = HyperTraceSdk.CONFIG.advertisingInterval

        val maxQueueTime: Long = HyperTraceSdk.CONFIG.maxPeripheralQueueTime
        val bmCheckInterval: Long = HyperTraceSdk.CONFIG.temporaryIdCheckInterval
        val healthCheckInterval: Long = HyperTraceSdk.CONFIG.bluetoothServiceHeartBeat
        val purgeInterval: Long = HyperTraceSdk.CONFIG.purgeRecordInterval
        val purgeTTL: Long = HyperTraceSdk.CONFIG.recordTTL

        val connectionTimeout: Long = HyperTraceSdk.CONFIG.deviceConnectionTimeout

        val blacklistDuration: Long = HyperTraceSdk.CONFIG.deviceBlacklistDuration

        const val infiniteScanning = false
        const val infiniteAdvertising = false

        const val useBlacklist = true
        const val bmValidityCheck = false
    }
}
