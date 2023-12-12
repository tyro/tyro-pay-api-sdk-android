package com.tyro.payapi.log

import android.content.pm.ApplicationInfo
import timber.log.Timber

internal object LoggerUtil {
    fun enableLogsIfDebug(applicationInfo: ApplicationInfo) {
        val isDebug = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

        if (isDebug) {
            // since we cant call this from the application as its a library, we must uproot the tree each time
            // otherwise we get duplicate logs everytime the activity is started
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
    }
}
