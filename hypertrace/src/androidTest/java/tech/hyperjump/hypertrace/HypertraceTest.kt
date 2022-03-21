package tech.hyperjump.hypertrace

import android.app.Notification
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.app.NotificationChannelCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.runner.RunWith
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecord
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class HypertraceTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private val testDispatcher: CoroutineContext = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)
    private lateinit var context: Context
    private lateinit var db: StreetPassRecordDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = StreetPassRecordDatabase.getDatabase(context)
        val config = HyperTraceSdk.Config(
                notificationChannelCreator = {
                    NotificationChannelCompat.Builder("", 0).build()
                },
                foregroundNotificationCreator = { Notification() },
                bluetoothFailedNotificationCreator = { Notification() },
                baseUrl = "",
                bleServiceUuid = UUID.randomUUID().toString(),
                bleCharacteristicUuid = UUID.randomUUID().toString(),
                organization = "TEST",
                userId = ""
        )
        HyperTraceSdk.setConfig(config)
        StreetPassRecordDatabase.TEST = true
    }

    @After
    fun cleanup() {
        db.recordDao().nukeDb()
    }

    @Test
    fun testCount() {
        testScope.runBlockingTest {
            //given
            val freshEncounter = StreetPassRecord("Test")
            db.recordDao().insert(freshEncounter)
            val freshEncounter2 = StreetPassRecord("Test2")
            db.recordDao().insert(freshEncounter2)
            val oldEncounter = StreetPassRecord("Test3")

            // backwards timestamp to configured recordTTL + 1s
            val backwardTime = HyperTraceSdk.CONFIG.recordTTL.minus(1_000L)
            oldEncounter.timestamp = backwardTime
            db.recordDao().insert(oldEncounter)

            // when count encounter before configured recordTTL
            val encounterCountSevenDaysAgo = HyperTraceSdk.countEncounters()

            // when count total encounter before now
            val totalEncounter = HyperTraceSdk.countEncounters(before = System.currentTimeMillis())

            // verify
            Assert.assertEquals(1, encounterCountSevenDaysAgo)
            Assert.assertEquals(3, totalEncounter)
        }
    }

    @Test
    fun testDelete_DefaultRecordTTL() {
        testScope.runBlockingTest {
            // given
            db.recordDao().insert(StreetPassRecord("Test"))
            val recordCount = HyperTraceSdk.countEncounters(before = System.currentTimeMillis())
            Assert.assertEquals(1, recordCount)

            // when remove encounters older than config recordTTL
            // default parameter
            HyperTraceSdk.removeEncounters()

            // verify
            val afterRemove = HyperTraceSdk.countEncounters(before = System.currentTimeMillis())
            Assert.assertEquals(1, afterRemove)
        }
    }

    @Test
    fun testDelete_Parameterized() {
        testScope.runBlockingTest {
            // given
            val freshEncounter = StreetPassRecord("Test")
            db.recordDao().insert(freshEncounter)
            val oldEncounter = StreetPassRecord("Test2")

            // backwards timestamp to configured recordTTL + 1s
            val backwardTime = HyperTraceSdk.CONFIG.recordTTL.minus(1_000L)
            oldEncounter.timestamp = backwardTime
            db.recordDao().insert(oldEncounter)

            val encounterBeforeDelete = HyperTraceSdk.countEncounters(before = System.currentTimeMillis())

            // when delete encounter before now
            // non default parameter
            HyperTraceSdk.removeEncounters(before = System.currentTimeMillis())
            val encounterAfterDelete = HyperTraceSdk.countEncounters(before = System.currentTimeMillis())

            // verify
            Assert.assertEquals(2, encounterBeforeDelete)
            Assert.assertEquals(0, encounterAfterDelete)
        }
    }
}
