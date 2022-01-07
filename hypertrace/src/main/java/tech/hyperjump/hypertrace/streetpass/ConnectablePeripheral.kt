package tech.hyperjump.hypertrace.streetpass

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConnectablePeripheral(
        var manuData: String,
        var transmissionPower: Int?,
        var rssi: Int
) : Parcelable

@Parcelize
data class PeripheralDevice(
        @SerializedName("modelP") val modelP: String,
        @SerializedName("address") val address: String?
) : Parcelable

@Parcelize
data class CentralDevice(
        @SerializedName("modelC") val modelC: String,
        @SerializedName("address") val address: String?
) : Parcelable

@Parcelize
data class ConnectionRecord(
        val version: Int,

        val msg: String,
        val org: String,

        val peripheral: PeripheralDevice,
        val central: CentralDevice,

        var rssi: Int,
        var txPower: Int?
) : Parcelable {
    override fun toString(): String {
        return "Central ${central.modelC} - ${central.address} ---> Peripheral ${peripheral.modelP} - ${peripheral.address}"
    }
}
