package com.diplomat

import android.app.Application
import com.diplomat.core.AppContainer
import com.diplomat.service.NotificationChannels

/**
 * Application entry point. Owns the [AppContainer] (manual DI) and registers
 * notification channels on startup.
 */
class DiplomatApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.registerAll(this)
    }
}
