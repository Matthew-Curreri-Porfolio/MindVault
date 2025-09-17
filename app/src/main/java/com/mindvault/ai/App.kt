package com.mindvault.ai

import android.app.Application
import android.content.Context

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.appContext = applicationContext
    }
}
object AppContextHolder {
    lateinit var appContext: Context
}
