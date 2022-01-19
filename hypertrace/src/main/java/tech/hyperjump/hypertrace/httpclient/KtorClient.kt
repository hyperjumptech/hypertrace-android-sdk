package tech.hyperjump.hypertrace.httpclient

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import tech.hyperjump.hypertrace.HyperTraceSdk

internal val ktorClient = HttpClient(OkHttp) {
    install(JsonFeature) { serializer = GsonSerializer() }
    engine {
        config {
            val configRunner = HyperTraceSdk.CONFIG.okHttpConfig
            if (configRunner != null) {
                this.apply(configRunner)
                return@config
            }
            val certificatePinnerConfig = HyperTraceSdk.CONFIG.certificatePinner
            if (certificatePinnerConfig != null) certificatePinner(certificatePinnerConfig)
        }
    }

    if (HyperTraceSdk.CONFIG.debug) {
        install(Logging) {
            level = LogLevel.BODY
        }
    }
}
