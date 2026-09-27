package com.beingolea.recordatorios

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.onesignal.OneSignal

class FamilyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createBirthdayChannel()
        OneSignal.initWithContext(this, BuildConfig.ONESIGNAL_APP_ID)
    }

    private fun createBirthdayChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BIRTHDAY_CHANNEL_ID,
                "Cumpleaños de la familia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de cumpleaños de la familia Beingolea"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val BIRTHDAY_CHANNEL_ID = "cumpleanos_familia"
    }
}
