package com.vault.vanishx

import android.app.Application
import com.vault.vanishx.data.push.RoomNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var roomNotificationHelper: RoomNotificationHelper

    override fun onCreate() {
        super.onCreate()
        setupLogging()
        roomNotificationHelper.ensureChannel()
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
