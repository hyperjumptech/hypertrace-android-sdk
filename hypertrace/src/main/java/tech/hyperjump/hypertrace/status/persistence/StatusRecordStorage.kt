package tech.hyperjump.hypertrace.status.persistence

import android.content.Context
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecordDatabase

class StatusRecordStorage(val context: Context) {

    val statusDao = StreetPassRecordDatabase.getDatabase(context).statusDao()

    suspend fun saveRecord(record: StatusRecord): Long {
        return statusDao.insert(record)
    }

    fun nukeDb() {
        statusDao.nukeDb()
    }

    fun getAllRecords(): List<StatusRecord> {
        return statusDao.getCurrentRecords()
    }

    suspend fun purgeOldRecords(before: Long) {
        statusDao.purgeOldRecords(before)
    }
}
