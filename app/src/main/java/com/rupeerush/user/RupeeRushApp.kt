package com.rupeerush.user

import android.app.Application
import com.rupeerush.user.core.service.NotificationHelper

class RupeeRushApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
