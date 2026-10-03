package com.antigravity.noichuan

import android.app.Application
import com.antigravity.noichuan.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class NoiChuanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NoiChuanApp)
            modules(appModule)
        }
    }
}
