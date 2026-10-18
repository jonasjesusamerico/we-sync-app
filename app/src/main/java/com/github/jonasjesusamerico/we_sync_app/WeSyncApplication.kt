package com.github.jonasjesusamerico.we_sync_app

import android.app.Application

class WeSyncApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
