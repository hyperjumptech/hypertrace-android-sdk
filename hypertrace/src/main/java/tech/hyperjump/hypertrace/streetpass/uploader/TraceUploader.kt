package tech.hyperjump.hypertrace.streetpass.uploader

import com.google.gson.annotations.SerializedName
import io.ktor.client.request.*
import io.ktor.http.*
import tech.hyperjump.hypertrace.HyperTraceSdk
import tech.hyperjump.hypertrace.httpclient.ktorClient
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecordDatabase

internal object TraceUploader {

    private const val TAG = "TraceUploader"
    private const val SERVICE_HANDSHAKE_PIN = "getHandshakePin"
    private const val SERVICE_UPLOAD_TOKEN = "getUploadToken"
    private const val SERVICE_UPLOAD_RECORDS = "uploadData"

    suspend fun getHandshakePin(): String {
        val url = HyperTraceSdk.CONFIG.baseUrl + SERVICE_HANDSHAKE_PIN
        return ktorClient.get(url) {
            parameter("uid", HyperTraceSdk.CONFIG.userId)
        }
    }

    suspend fun uploadEncounterRecords(secret: String) {
        val uploadToken = getUploadToken(secret)
        startStreetPassUpload(uploadToken)
    }

    private suspend fun getUploadToken(secret: String): String {
        val url = HyperTraceSdk.CONFIG.baseUrl + SERVICE_UPLOAD_TOKEN
        return ktorClient.get(url) {
            parameter("uid", HyperTraceSdk.CONFIG.userId)
            parameter("data", secret)
        }
    }

    private suspend fun startStreetPassUpload(uploadToken: String) {
        val records = StreetPassRecordDatabase.getDatabase(HyperTraceSdk.appContext)
                .recordDao()
                .getCurrentRecords()
                .map { record ->
                    EncounterPayload(
                            v = record.v,
                            msg = record.msg,
                            modelC = record.modelC,
                            modelP = record.modelP,
                            org = record.org,
                            rssi = record.rssi,
                            timestamp = record.timestamp / 1000,
                            txPower = record.txPower,
                    )
                }

        val uploadBody = StreetPassUploadBody(
                uid = HyperTraceSdk.CONFIG.userId,
                uploadToken = uploadToken,
                traces = records,
        )

        val url = HyperTraceSdk.CONFIG.baseUrl + SERVICE_UPLOAD_RECORDS
        ktorClient.post<String>(url) {
            contentType(ContentType.Application.Json)
            body = uploadBody
        }
    }

    internal data class StreetPassUploadBody(
            @SerializedName("uid") val uid: String,
            @SerializedName("uploadToken") val uploadToken: String,
            @SerializedName("traces") val traces: List<EncounterPayload>,
    )

    internal data class EncounterPayload(
            @SerializedName("v")
            val v: Int,

            @SerializedName("msg")
            val msg: String,

            @SerializedName("org")
            val org: String,

            @SerializedName("modelP")
            val modelP: String,

            @SerializedName("modelC")
            val modelC: String,

            @SerializedName("rssi")
            val rssi: Int,

            @SerializedName("txPower")
            val txPower: Int?,

            @SerializedName("timestamp")
            val timestamp: Long
    )
}
