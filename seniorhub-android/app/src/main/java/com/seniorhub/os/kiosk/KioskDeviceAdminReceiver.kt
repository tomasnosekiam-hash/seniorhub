package com.seniorhub.os.kiosk

import android.app.admin.DeviceAdminReceiver

/** Provisioning: `adb shell dpm set-device-owner com.seniorhub.os/.kiosk.KioskDeviceAdminReceiver` */
class KioskDeviceAdminReceiver : DeviceAdminReceiver()
