package tech.hyperjump.hypertrace

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
        StreetPassRecordDatabase.TEST = true
    }

    @After
    fun cleanup() {
        db.recordDao().nukeDb()
    }

    @Test
    fun testWrite() {
        testScope.runBlockingTest {
            db.recordDao().insert(StreetPassRecord("Test"))
        }

        testScope.runBlockingTest {
            val recordCount = HyperTraceSdk.countEncounter()
            Assert.assertEquals(1, recordCount)
        }
    }

    @Test
    fun testWriteDouble() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = StreetPassRecordDatabase.getDatabase(context)
        testScope.runBlockingTest {
            db.recordDao().insert(StreetPassRecord("Test"))
            db.recordDao().insert(StreetPassRecord("Test2"))
        }

        testScope.runBlockingTest {
            val recordCount = HyperTraceSdk.countEncounter()
            Assert.assertEquals(2, recordCount)
        }
    }

    @Test
    fun testDelete() {
        testScope.runBlockingTest {
            db.recordDao().insert(StreetPassRecord("Test"))
        }

        testScope.runBlockingTest {
            val recordCount = HyperTraceSdk.countEncounter()
            Assert.assertEquals(1, recordCount)
        }

        db.recordDao().nukeDb()
        testScope.runBlockingTest {
            val recordCount = HyperTraceSdk.countEncounter()
            Assert.assertEquals(0, recordCount)
        }
    }
}
