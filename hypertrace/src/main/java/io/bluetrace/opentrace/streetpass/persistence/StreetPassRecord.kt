package io.bluetrace.opentrace.streetpass.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "record_table")
data class StreetPassRecord(
        @ColumnInfo(name = "v")
        @SerializedName("v")
        var v: Int,

        @ColumnInfo(name = "msg")
        @SerializedName("msg")
        var msg: String,

        @ColumnInfo(name = "org")
        @SerializedName("org")
        var org: String,

        @ColumnInfo(name = "modelP")
        @SerializedName("modelP")
        val modelP: String,

        @ColumnInfo(name = "modelC")
        @SerializedName("modelC")
        val modelC: String,

        @ColumnInfo(name = "rssi")
        @SerializedName("rssi")
        val rssi: Int,

        @ColumnInfo(name = "txPower")
        @SerializedName("txPower")
        val txPower: Int?

) {

    constructor(msg: String) : this(v = 0, msg, org = "", modelP = "", modelC = "", rssi = 0, txPower = null)

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "timestamp")
    @SerializedName("timestamp")
    var timestamp: Long = System.currentTimeMillis()

}
