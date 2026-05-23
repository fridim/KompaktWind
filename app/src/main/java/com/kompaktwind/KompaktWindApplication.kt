package com.kompaktwind

import android.app.Application

class KompaktWindApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
