package com.kbretro.babyregistry

import android.app.Application
import com.kbretro.babyregistry.reminders.ReminderScheduler
import com.kbretro.babyregistry.reminders.TimerNotifier
import com.kbretro.babyregistry.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class BabyRegistryApp : Application() {
    @Inject lateinit var timerNotifier: TimerNotifier
    @Inject lateinit var widgetUpdater: WidgetUpdater

    /** Ámbito de la aplicación para observar la base de datos mientras el proceso vive. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        TimerNotifier.createChannel(this)
        timerNotifier.start(appScope)
        widgetUpdater.start(appScope)
    }
}
