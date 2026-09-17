package com.secondbrain.android.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Queues server reconciliation after reboot without doing network I/O on the broadcast thread. */
class ReminderRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderRecovery.enqueue(context)
        }
    }
}
