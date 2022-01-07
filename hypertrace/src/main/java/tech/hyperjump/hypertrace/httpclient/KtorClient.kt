package tech.hyperjump.hypertrace.httpclient

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import tech.hyperjump.hypertrace.BuildConfig

internal val ktorClient = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = GsonSerializer()
    }

    if (BuildConfig.DEBUG) {
        install(Logging) {
            level = LogLevel.BODY
        }
    }
}
