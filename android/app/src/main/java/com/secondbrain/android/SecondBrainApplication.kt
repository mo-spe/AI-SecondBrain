package com.secondbrain.android

import android.app.Application
import com.secondbrain.android.reminder.ReminderRecovery
import dagger.hilt.android.HiltAndroidApp

/** App entry point that enables one shared dependency graph for all feature flows. */
@HiltAndroidApp
class SecondBrainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderRecovery.enqueue(this)
    }
}
