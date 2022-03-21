package tech.hyperjump.hypertrace.startup

import android.content.Context
import androidx.startup.Initializer
import tech.hyperjump.hypertrace.HyperTraceSdk

class HypertraceInitializer : Initializer<HyperTraceSdk> {
    override fun create(context: Context): HyperTraceSdk {
        HyperTraceSdk.appContext = context
        return HyperTraceSdk
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
