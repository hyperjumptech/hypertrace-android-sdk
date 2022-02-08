package tech.hyperjump.hypertrace

import android.app.Notification
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.ServiceTestRule
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class HyperTraceNotification {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @get:Rule
    val grantPermissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    )

    private lateinit var bluetoothAdapter: BluetoothAdapter

    @RelaxedMockK
    private lateinit var mockNotificationForegroundSuccess: (context: Context) -> Notification

    @RelaxedMockK
    private lateinit var mockNotificationBluetoothFail: (context: Context) -> Notification

    @RelaxedMockK
    private lateinit var mockNotificationChannelCreator: () -> NotificationChannelCompat

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val context = InstrumentationRegistry.getInstrumentation().context
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter
    }

    @Test
    fun startSdkWithBluetoothDisabledWillThrow() {
        bluetoothAdapter.disable()
        val cfg = HyperTraceSdk.Config(
                baseUrl = "https://example.com/",
                bleCharacteristicUuid = UUID.randomUUID().toString(),
                bleServiceUuid = UUID.randomUUID().toString(),
                bluetoothFailedNotificationCreator = mockNotificationBluetoothFail,
                foregroundNotificationCreator = mockNotificationForegroundSuccess,
                notificationChannelCreator = mockNotificationChannelCreator,
                organization = "TEST",
                userId = "123456789012345678901",
        )

        Assert.assertThrows(Exception::class.java) {
            HyperTraceSdk.startService(cfg)
        }
    }
}
