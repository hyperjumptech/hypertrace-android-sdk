package tech.hyperjump.example

import android.Manifest
import android.app.Notification
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions
import tech.hyperjump.hypertrace.HyperTraceSdk
import tech.hyperjump.hypertrace.scandebug.ScanDebugActivity
import tech.hyperjump.hypertrace.streetpassdebug.StreetPassDebugActivity

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (requestBluetooth()) setupOpenTrace()
        button_scan.setOnClickListener {
            startActivity(Intent(this, ScanDebugActivity::class.java))
        }
        button_street_pass.setOnClickListener {
            startActivity(Intent(this, StreetPassDebugActivity::class.java))
        }
        button_handshake_pin.setOnClickListener { getHandshakePin() }
        button_upload_contact.setOnClickListener { uploadContactTrace() }
    }

    private fun requestBluetooth(): Boolean {
        val permissions = EasyPermissions.hasPermissions(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH,
        )
        if (permissions) {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            if (btManager?.adapter?.isEnabled == false) btManager.adapter?.enable()
            return permissions
        }

        EasyPermissions.requestPermissions(
                this,
                "App needs permissions to work properly",
                999,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
        )

        return permissions
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        setupOpenTrace()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        toast("Permissions denied")
    }

    private fun getHandshakePin() {
        lifecycleScope.launchWhenResumed {
            progress_bar.visibility = View.VISIBLE
            button_handshake_pin.visibility = View.GONE
            val pin = HyperTraceSdk.getHandshakePin()
            if (pin != null) {
                tv_pin.text = pin
                tv_pin.visibility = View.VISIBLE
                progress_bar.visibility = View.GONE
            } else {
                toast("Failed to get PIN")
                progress_bar.visibility = View.GONE
                button_handshake_pin.visibility = View.VISIBLE
            }
        }
    }

    private fun uploadContactTrace() {
        lifecycleScope.launchWhenResumed {
            val secret = et_secret.text.toString()
            HyperTraceSdk.uploadEncounterRecords(
                    secret,
                    onSuccess = { toast("Contact trace successfully uploaded") },
                    onError = { toast("Failed to upload") }
            )
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun setupOpenTrace() {
        HyperTraceSdk.startService(buildConfig())
    }

    private fun buildConfig(): HyperTraceSdk.Config {
        return HyperTraceSdk.Config(
                userId = generateUserId(21),
                organization = "ID_HYPERJUMP",
                baseUrl = "http://20.212.164.71:8080/",
                bleServiceUuid = "A6BA4286-C550-4794-A888-9467EF0B31A8",
                bleCharacteristicUuid = "D1034710-B11E-42F2-BCA3-F481177D5BB2",
                foregroundNotificationCreator = this::createForegroundNotification,
                bluetoothFailedNotificationCreator = this::createBluetoothFailedNotification,
                notificationChannelCreator = this::createNotificationChannel,
        )
    }

    private fun generateUserId(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return List(length) {
            allowedChars.random()
        }.joinToString()
    }

    private fun createForegroundNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Hypertrace - Running")
                .setContentText("Hypertrace is running")
                .build()
    }

    private fun createBluetoothFailedNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Hypertrace - Failed")
                .setContentText("Bluetooth and/or location is disabled.")
                .build()
    }

    private fun createNotificationChannel(): NotificationChannelCompat {
        return NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Contact tracing notification")
                .build()
    }

    companion object {
        private const val CHANNEL_ID = "tech.hyperjump.hypertrace.notification-channel"
    }
}