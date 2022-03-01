# Android Hypertrace - Kotlin implementation of OpenTrace by Hyperjump

Kotlin OpenTrace implementation based on [BlueTrace specification](https://bluetrace.io/static/bluetrace_whitepaper-938063656596c104632def383eb33b3c.pdf).

### Table of Content

- [Requirements](#requirements)
- [Installation](#installation)
- [How to use](#how-to-use)
  - [Start background service](#start-background-service)
    - [Available configuration](#available-configuration)
  - [Stop background service](#stop-background-service)
  - [Get handshake PIN](#get-handshake-pin)
  - [Upload contact encounter](#upload-contact-encounter)
- [Debugging](#debugging)
- [Troubleshooting](#troubleshooting)
- [Protocol version](#protocol-version)
- [Security enhancement](#security-enhancements)
- [Statement from Google](#statement-from-google)
- [Changelog](#changelog)

### Requirements

- Android API level 23+ (Android M)
- Kotlin 1.5.31
- A device with Bluetooth Low Energy (BLE) support.
- This SDK requires the application be granted following permissions:
  - `android.permission.ACCESS_FINE_LOCATION`
  - `android.permission.ACCESS_COARSE_LOCATION`
  - `android.permission.BLUETOOTH`

### Installation

1. Download [latest release](https://github.com/hyperjumptech/hypertrace-android-sdk/release/latest/release/latest) file.
2. Place `hypertrace-release.aar` file into your `app/libs` directory.
3. In your `app/build.gradle` file, make sure you have the following line:

```
implementation fileTree(dir: "libs", include: ["*.jar", "*.aar"])
```

4. For Android Studio users, sync your gradle project and it's ready to use.

### How to use

#### Start background service

```kotlin
val config = HypertraceSdk.Config()
HypertraceSdk.startService(config)
```

##### Available configuration

| Field                              | Type                 | Description                                                                                                                     | Mandatory | Default       |
| :--------------------------------- | :------------------- | :------------------------------------------------------------------------------------------------------------------------------ | :-------- | :------------ |
| notificationChannelCreator         | Function             | Kotlin higher-order function for SDK to create android notification channel. (Required for API level 26 / Android O).           | **YES**   | -             |
| foregroundNotificationCreator      | Function             | Kotlin higher-order function for SDK to create notification when service is actively running.                                   | **YES**   | -             |
| bluetoothFailedNotificationCreator | Function             | Kotlin higher-order function for SDK to create notification when service fails to access bluetooth and location permissions.    | **YES**   | -             |
| userId                             | string               | Main application's user ID. **Must be** 21 characters.                                                                          | **YES**   | -             |
| organization                       | string               | Application's organization name. Typically, this is a combination of `COUNTRY_CODE` and short organization name.                | **YES**   | -             |
| baseUrl                            | string               | URL for [Hypertrace server](https://github.com/hyperjumptech/hypertrace) implementation. **Must end** with slash `/` character. | **YES**   | -             |
| bleServiceUuid                     | string               | BLE service UUID. **Must be** a valid UUID.                                                                                     | **YES**   | -             |
| bleCharacteristicUuid              | string               | BLE characteristic UUID. **Must be** a valid UUID.                                                                              | **YES**   | -             |
| debug                              | boolean              | Enable showing street pass list and bluetooth scanning activity                                                                 | **NO**    | -             |
| keepAliveService                   | boolean              | If `true`, Hypertrace will try to restart service everytime it is killed.                                                       | **NO**    | `false`       |
| scanDuration                       | long                 | Duration of each bluetooth scan action in **miliseconds.** Default to 10 seconds.                                               | **NO**    | 10_000        |
| minScanInterval                    | long                 | Minimum scan interval in **miliseconds.** Randomized between minScanInterval and maxScanInterval. Default to 30 seconds.        | **NO**    | 30_000        |
| maxScanInterval                    | long                 | Maximum scan interval in **miliseconds.** Randomized between minScanInterval and maxScanInterval. Default to 40 seconds.        | **NO**    | 40_000        |
| advertisingDuration                | long                 | Duration of each bluetooth advertising action in **miliseconds.** Default to 30 minutes.                                        | **NO**    | 180_000       |
| advertisingInterval                | long                 | Interval between bluetooth advertising action in **miliseconds.** Default to 6 seconds.                                         | **NO**    | 6_000         |
| purgeRecordInterval                | long                 | Interval between purge action of encounter's records in **miliseconds.** Default to 24 hours.                                   | **NO**    | 86_400_000    |
| recordTTL                          | long                 | The lifetime of encounter's records in **miliseconds.** Default to 21 days.                                                     | **NO**    | 1_814_400_000 |
| maxPeripheralQueueTime             | long                 | Maximum time of a peripheral to wait to be processed in **miliseconds.** Default to 10 seconds.                                 | **NO**    | 10_000        |
| deviceConnectionTimeout            | long                 | Maximum time of a peripheral read-write action in **miliseconds.** Default to 6 seconds.                                        | **NO**    | 6_000         |
| deviceBlacklistDuration            | long                 | Maximum time of a device to be blacklisted time in **miliseconds.** Default to 1.5 minutes.                                     | **NO**    | 90_000        |
| temporaryIdCheckInterval           | long                 | Interval between temporary IDs' supply check **miliseconds.** Default to 10 minutes.                                            | **NO**    | 600_000       |
| bluetoothServiceHeartBeat          | long                 | Interval between OpenTrace bluetooth service check **miliseconds.** Default to 15 minutes.                                      | **NO**    | 900_000       |
| certificatePinner                  | CertificatePinner    | Helper for certificate pinning provided by OkHttp. See [**Security Enhancements.**](#security-enhancements)                     | **NO**    | null          |
| okHttpConfig                       | OkHttpClient.Builder | For a complete control of SDK's OkHttpClient.                                                                                   | **NO**    | null          |

#### Stop background service

`stopService` will stop Hypertrace background service.

```kotlin
HypertraceSdk.stopService()
```

#### Get handshake PIN

`getHandshakePin` will fetch a PIN from server for identification from authority.

**Return**: String, handshake PIN.

**Throw**: Exception on failures.

```kotlin
HypertraceSdk.getHandshakePin()
```

#### Upload contact encounter

`uploadEncounterRecords` will upload recorded BLE encounters in local to server.

**Return**: Unit / void.

**Throw**: Exception on failures.

Params:

- secret: temporary password provided by authority.

```kotlin
HypertraceSdk.uploadEncounterRecords(secret, onSuccess = {}, onError = {})
```

#### Count encounter

`countEncounters` will return encounter record count. By default, this method only count the expired records, respective to **recordTTL**. See [configuration](#available-configuration).
**Param**: `before`, timestamp in millis.
**Return**: Record count before given timestamp, **recordTTL** by default.

#### Remove encounter

`removeEncounters` will delete encounter records. By default, this method only delete expired records, respective to **recordTTL**. See [configuration](#available-configuration).

**Param**: `before`, timestamp in millis.
**Return**: Unit / void.

### Debugging

Hypertrace SDK provides the following classes for debugging purposes:

- `tech.hyperjump.hypertrace.scandebug.ScanDebugActivity` for bluetooth scanning status.
- `tech.hyperjump.hypertrace.scandebug.StreetPassDebugActivity` for contact tracing records in local.

### Troubleshooting

- Foreground service get killed when app is paused on device model XXX.

  For non-AOSP users, you need to whitelist the application from OS' power manager. For further information, please refer to [Don't Kill My App](https://dontkillmyapp.com/) website.

### Protocol Version

Protocol version used should be 2 (or above)
Version 1 of the protocol has been deprecated

---

### Security Enhancements

Hypertrace uses [OkHttp v4.x](https://square.github.io/okhttp/) under the hood. We provide configurable certificate pinner, for implementation, refer to [CertificatePinner documentation.](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-certificate-pinner/)

---

### Statement from Google

The following is a statement from Google:
"At Google Play we take our responsibility to provide accurate and relevant information for our users very seriously. For that reason, we are currently only approving apps that reference COVID-19 or related terms in their store listing if the app is published, commissioned, or authorized by an official government entity or public health organization, and the app does not contain any monetization mechanisms such as ads, in-app products, or in-app donations. This includes references in places such as the app title, description, release notes, or screenshots.
For more information visit [https://android-developers.googleblog.com/2020/04/google-play-updates-and-information.html](https://android-developers.googleblog.com/2020/04/google-play-updates-and-information.html)"

---

### ChangeLog

**0.9.0**

- First release.
