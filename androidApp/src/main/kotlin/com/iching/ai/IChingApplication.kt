package com.iching.ai

import android.app.Application
import com.iching.ai.di.initKoin
import org.koin.android.ext.koin.androidContext

class IChingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@IChingApplication)
        }
    }
}
