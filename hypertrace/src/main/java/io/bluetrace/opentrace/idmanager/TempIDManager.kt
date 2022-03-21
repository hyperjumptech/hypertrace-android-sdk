package io.bluetrace.opentrace.idmanager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.client.request.*
import org.json.JSONObject
import tech.hyperjump.hypertrace.HyperTraceSdk
import io.bluetrace.opentrace.Preference
import tech.hyperjump.hypertrace.httpclient.ktorClient
import io.bluetrace.opentrace.logging.CentralLog
import io.bluetrace.opentrace.services.BluetoothMonitoringService.Companion.bmValidityCheck
import java.io.File
import java.util.*

internal object TempIDManager {

    private const val TAG = "TempIDManager"
    private const val SERVICE_TEMP_ID = "getTempIDs"

    private fun storeTemporaryIDs(context: Context, packet: String) {
        CentralLog.d(TAG, "[TempID] Storing temporary IDs into internal storage...")
        val file = File(context.filesDir, "tempIDs")
        file.writeText(packet)
    }

    fun retrieveTemporaryID(context: Context): TemporaryID? {
        val file = File(context.filesDir, "tempIDs")
        if (file.exists()) {
            val readBack = file.readText()
            CentralLog.d(TAG, "[TempID] fetched broadcastmessage from file:  $readBack")
            val tempIDArrayList = convertToTemporaryIDs(readBack)
            val tempIDQueue = convertToQueue(tempIDArrayList)
            return getValidOrLastTemporaryID(
                    context,
                    tempIDQueue
            )
        }
        return null
    }

    private fun getValidOrLastTemporaryID(
            context: Context,
            tempIDQueue: Queue<TemporaryID>
    ): TemporaryID? {
        CentralLog.d(TAG, "[TempID] Retrieving Temporary ID")
        val currentTime = System.currentTimeMillis()

        var pop = 0
        while (tempIDQueue.size > 1) {
            val tempID = tempIDQueue.peek()
            tempID?.print()

            if (tempID?.isValidForCurrentTime() == true) {
                CentralLog.d(TAG, "[TempID] Breaking out of the loop")
                break
            }

            tempIDQueue.poll()
            pop++
        }

        val foundTempID = tempIDQueue.peek()
        val foundTempIDStartTime = (foundTempID?.startTime ?: 0) * 1000
        val foundTempIDExpiryTime = (foundTempID?.expiryTime ?: 0) * 1000

        CentralLog.d(TAG, "[TempID Total number of items in queue: ${tempIDQueue.size}")
        CentralLog.d(TAG, "[TempID Number of items popped from queue: $pop")
        CentralLog.d(TAG, "[TempID] Current time: $currentTime")
        CentralLog.d(TAG, "[TempID] Start time: $foundTempIDStartTime")
        CentralLog.d(TAG, "[TempID] Expiry time: $foundTempIDExpiryTime")
        CentralLog.d(TAG, "[TempID] Updating expiry time")
        Preference.putExpiryTimeInMillis(context, foundTempIDExpiryTime)
        return foundTempID
    }

    private fun convertToTemporaryIDs(tempIDString: String): Array<TemporaryID> {
        val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

        val tempIDResult = gson.fromJson(tempIDString, Array<TemporaryID>::class.java)
        CentralLog.d(
                TAG,
                "[TempID] After GSON conversion: ${tempIDResult[0].tempID} ${tempIDResult[0].startTime}"
        )

        return tempIDResult
    }

    private fun convertToQueue(tempIDArray: Array<TemporaryID>): Queue<TemporaryID> {
        CentralLog.d(TAG, "[TempID] Before Sort: ${tempIDArray[0]}")

        //Sort based on start time
        tempIDArray.sortBy {
            return@sortBy it.startTime
        }
        CentralLog.d(TAG, "[TempID] After Sort: ${tempIDArray[0]}")

        //Preserving order of array which was sorted
        val tempIDQueue: Queue<TemporaryID> = LinkedList<TemporaryID>()
        for (tempID in tempIDArray) {
            tempIDQueue.offer(tempID)
        }

        CentralLog.d(TAG, "[TempID] Retrieving from Queue: ${tempIDQueue.peek()}")
        return tempIDQueue
    }

    suspend fun getTemporaryIDs(context: Context, onComplete: () -> Unit) {
        try {
            val responseJson = ktorClient.get<String>(HyperTraceSdk.CONFIG.baseUrl + SERVICE_TEMP_ID) {
                parameter("uid", HyperTraceSdk.CONFIG.userId)
            }
            val jsonObject = JSONObject(responseJson)
            val tempIdsJson = jsonObject.getJSONArray("tempIDs")
            storeTemporaryIDs(context, tempIdsJson.toString())
            val refreshTime = jsonObject.getLong("refreshTime")
            Preference.putNextFetchTimeInMillis(context, refreshTime * 1000)
        } catch (t: Throwable) {
            CentralLog.e(TAG, t.message ?: "")
        }
        onComplete()
    }

    fun needToUpdate(context: Context): Boolean {
        val nextFetchTime =
                Preference.getNextFetchTimeInMillis(context)
        val currentTime = System.currentTimeMillis()

        val update = currentTime >= nextFetchTime
        CentralLog.i(
                TAG,
                "Need to update and fetch TemporaryIDs? $nextFetchTime vs $currentTime: $update"
        )
        return update
    }

    fun needToRollNewTempID(context: Context): Boolean {
        val expiryTime =
                Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime >= expiryTime
        CentralLog.d(TAG, "[TempID] Need to get new TempID? $expiryTime vs $currentTime: $update")
        return update
    }

    //Can Cleanup, this function always return true
    fun bmValid(context: Context): Boolean {
        val expiryTime =
                Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime < expiryTime

        if (bmValidityCheck) {
            CentralLog.w(TAG, "Temp ID is valid")
            return update
        }

        return true
    }
}
