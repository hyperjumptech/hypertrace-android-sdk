# Android Hypertrace - Kotlin implementation of OpenTrace by Hyperjump

### Requirements

- Android API level 23+ (Android M)
- Kotlin 1.5.31
- A device with Bluetooth Low Energy (BLE) support.
- This SDK requires the application be granted following permissions:
  - `android.permission.ACCESS_FINE_LOCATION`
  - `android.permission.ACCESS_COARSE_LOCATION`
  - `android.permission.BLUETOOTH`

### Installation

1. Download [latest release](./release/hypertrace-release.aar) file.
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

| Field                              | Type     | Description                                                                                                                     | Mandatory | Default |
| :--------------------------------- | :------- | :------------------------------------------------------------------------------------------------------------------------------ | :-------- | :------ |
| notificationChannelCreator         | Function | Kotlin higher-order function for SDK to create android notification channel. (Required for API level 26 / Android O).           | **YES**   | -       |
| foregroundNotificationCreator      | Function | Kotlin higher-order function for SDK to create notification when service is actively running.                                   | **YES**   | -       |
| bluetoothFailedNotificationCreator | Function | Kotlin higher-order function for SDK to create notification when service fails to access bluetooth and location permissions.    | **YES**   | -       |
| userId                             | string   | Main application's user ID. **Must be** 21 characters.                                                                          | **YES**   | -       |
| organization                       | string   | Application's organization name. Typically, this is a combination of `COUNTRY_CODE` and short organization name.                | **YES**   | -       |
| baseUrl                            | string   | URL for [Hypertrace server](https://github.com/hyperjumptech/hypertrace) implementation. **Must end** with slash `/` character. | **YES**   | -       |
| bleServiceUuid                     | string   | BLE service UUID. **Must be** a valid UUID.                                                                                     | **YES**   | -       |
| bleCharacteristicUuid              | string   | BLE characteristic UUID. **Must be** a valid UUID.                                                                              | **YES**   | -       |
| debug                              | boolean  | Enable showing street pass list and bluetooth scanning activity                                                                 | **NO**    | -       |
| keepAliveService                   | boolean  | If `true`, Hypertrace will try to restart service everytime it is killed.                                                       | **NO**    | `false` |

#### Get handshake PIN

`getHandshakePin` will fetch a PIN from server for identification from authority.
Returns `string` or `null` if failed.

```kotlin
HypertraceSdk.getHandshakePin()
```

#### Upload contact encounter

`uploadEncounterRecords` will upload recorded BLE encounters in local to server.
Params:

- secret: temporary password provided by authority.
- onSuccess: kotlin higher-order function callback on successful uploads.
- onError: kotlin higher-order function callback on failed uploads.

```kotlin
HypertraceSdk.uploadEncounterRecords(secret, onSuccess = {}, onError = {})
```

### Debugging

Hypertrace SDK provides the following classes for debugging purposes:

- `tech.hyperjump.hypertrace.scandebug.ScanDebugActivity` for bluetooth scanning status.
- `tech.hyperjump.hypertrace.scandebug.StreetPassDebugActivity` for contact tracing records in local.

### Toubleshooting

- Foreground service get killed when app is paused on device model XXX.
  For non-AOSP users, you need to whitelist the application from OS' power manager. For further information, please refer to [Don't Kill My App](https://dontkillmyapp.com/) website.

### Protocol Version

Protocol version used should be 2 (or above)
Version 1 of the protocol has been deprecated

---

### Security Enhancements

SSL pinning is not included as part of the repo.
It is recommended to add in a check for SSL certificate returned by the backend.

---

### Statement from Google

The following is a statement from Google:
"At Google Play we take our responsibility to provide accurate and relevant information for our users very seriously. For that reason, we are currently only approving apps that reference COVID-19 or related terms in their store listing if the app is published, commissioned, or authorized by an official government entity or public health organization, and the app does not contain any monetization mechanisms such as ads, in-app products, or in-app donations. This includes references in places such as the app title, description, release notes, or screenshots.
For more information visit [https://android-developers.googleblog.com/2020/04/google-play-updates-and-information.html](https://android-developers.googleblog.com/2020/04/google-play-updates-and-information.html)"

---

### ChangeLog

**0.9.0**

- First release.
