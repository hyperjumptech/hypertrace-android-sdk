package tech.hyperjump.hypertrace.protocol.v2

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import tech.hyperjump.hypertrace.streetpass.PeripheralDevice

//acting as peripheral
data class V2ReadRequestPayload(
        @SerializedName("v") val v: Int,
        @SerializedName("id") val id: String,
        @SerializedName("o") val o: String,
        @SerializedName("peripheral") val peripheral: PeripheralDevice
) {
    val mp = peripheral.modelP

    fun getPayload(): ByteArray {
        return gson.toJson(this).toByteArray(Charsets.UTF_8)
    }

    companion object {
        val gson: Gson = GsonBuilder()
                .disableHtmlEscaping().create()

        fun fromPayload(dataBytes: ByteArray): V2ReadRequestPayload {
            val dataString = String(dataBytes, Charsets.UTF_8)
            return gson.fromJson(dataString, V2ReadRequestPayload::class.java)
        }
    }
}
