package io.bluetrace.opentrace.idmanager

import com.google.gson.annotations.SerializedName
import io.bluetrace.opentrace.logging.CentralLog

data class TemporaryID(
        @SerializedName("startTime") val startTime: Long,
        @SerializedName("tempID") val tempID: String,
        @SerializedName("expiryTime") val expiryTime: Long
) {

    fun isValidForCurrentTime(): Boolean {
        var currentTime = System.currentTimeMillis()
        return ((currentTime > (startTime * 1000)) && (currentTime < (expiryTime * 1000)))
    }

    fun print() {
        var tempIDStartTime = startTime * 1000
        var tempIDExpiryTime = expiryTime * 1000
        CentralLog.d(
                TAG,
                "[TempID] Start time: ${tempIDStartTime}"
        )
        CentralLog.d(
                TAG,
                "[TempID] Expiry time: ${tempIDExpiryTime}"
        )
    }

    companion object {
        private const val TAG = "TempID"
    }
}
