package com.vault.vanishx

import android.app.Application
import com.vault.vanishx.domain.model.InviteUriCodec
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
        InviteUriCodec.httpsHost = BuildConfig.INVITE_HTTPS_HOST
        setupLogging()
        roomNotificationHelper.ensureChannel()
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
