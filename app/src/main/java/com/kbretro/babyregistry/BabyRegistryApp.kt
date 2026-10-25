package com.kbretro.babyregistry

import android.app.Application
import com.kbretro.babyregistry.reminders.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BabyRegistryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
