package io.bluetrace.opentrace.logging

import android.os.PowerManager
import android.util.Log
import tech.hyperjump.hypertrace.HyperTraceSdk


class CentralLog {

    companion object {

        var pm: PowerManager? = null

        fun setPowerManager(powerManager: PowerManager) {
            pm = powerManager
        }

        private fun shouldLog(): Boolean {
            return HyperTraceSdk.CONFIG.debug
        }

        private fun getIdleStatus(): String {

            return if (true == pm?.isDeviceIdleMode) {
                " IDLE "
            } else {
                " NOT-IDLE "
            }
        }

        fun d(tag: String, message: String) {
            if (!shouldLog()) {
                return
            }

            Log.d(tag, getIdleStatus() + message)
        }

        fun d(tag: String, message: String, e: Throwable?) {
            if (!shouldLog()) {
                return
            }

            Log.d(tag, getIdleStatus() + message, e)
        }


        fun w(tag: String, message: String) {
            if (!shouldLog()) {
                return
            }

            Log.w(tag, getIdleStatus() + message)
        }

        fun i(tag: String, message: String) {
            if (!shouldLog()) {
                return
            }

            Log.i(tag, getIdleStatus() + message)
        }

        fun e(tag: String, message: String) {
            if (!shouldLog()) {
                return
            }

            Log.e(tag, getIdleStatus() + message)
        }

    }

}
