package tech.hyperjump.hypertrace.streetpass.uploader

import com.google.gson.annotations.SerializedName
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.json.JSONObject
import tech.hyperjump.hypertrace.HyperTraceSdk
import tech.hyperjump.hypertrace.httpclient.ktorClient
import tech.hyperjump.hypertrace.logging.CentralLog
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecord
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecordDatabase

internal object TraceUploader {

    private const val TAG = "TraceUploader"
    private const val SERVICE_HANDSHAKE_PIN = "getHandshakePin"
    private const val SERVICE_UPLOAD_TOKEN = "getUploadToken"
    private const val SERVICE_UPLOAD_RECORDS = "uploadData"

    suspend fun getHandshakePin(): String? {
        val url = HyperTraceSdk.CONFIG.baseUrl + SERVICE_HANDSHAKE_PIN
        return try {
            val result = ktorClient.get<String>(url) {
                parameter("uid", HyperTraceSdk.CONFIG.userId)
            }
            /* return */ JSONObject(result).getString("pin")
        } catch (t: Throwable) {
            CentralLog.e(TAG, t.message ?: "")
            /* return */ null
        }
    }

    suspend fun uploadEncounterRecords(secret: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val uploadToken = getUploadToken(secret)
                ?: return onError()
        startStreetPassUpload(uploadToken, onSuccess, onError)
    }

    private suspend fun getUploadToken(secret: String): String? {
        val url = HyperTraceSdk.CONFIG.baseUrl + SERVICE_UPLOAD_TOKEN
        return try {
            val result = ktorClient.get<String>(url) {
                parameter("uid", HyperTraceSdk.CONFIG.userId)
                parameter("data", secret)
            }
            /* return */ JSONObject(result).getString("token")
        } catch (t: Throwable) {
            CentralLog.e(TAG, t.message ?: "")
            /* return */ null
        }
    }

    private suspend fun startStreetPassUpload(
            uploadToken: String,
            onSuccess: () -> Unit,
            onError: () -> Unit,
    ) {
        val records = StreetPassRecordDatabase.getDatabase(HyperTraceSdk.appContext)
                .recordDao()
                .getCurrentRecords()
                .map { record ->
                    record.timestamp = record.timestamp / 1000
                    return@map record
                }

        val uploadBody = StreetPassUploadBody(
                uid = HyperTraceSdk.CONFIG.userId,
                uploadToken = uploadToken,
                traces = records,
        )

        val url = HyperTraceSdk.CONFIG.baseUrl + SERVICE_UPLOAD_RECORDS
        try {
            ktorClient.get<String>(url) {
                contentType(ContentType.Application.Json)
                body = uploadBody
            }
            onSuccess()
        } catch (t: Throwable) {
            CentralLog.e(TAG, t.message ?: "")
            onError()
        }
    }

    internal data class StreetPassUploadBody(
            @SerializedName("uid") val uid: String,
            @SerializedName("uploadToken") val uploadToken: String,
            @SerializedName("traces") val traces: List<StreetPassRecord>,
    )
}
