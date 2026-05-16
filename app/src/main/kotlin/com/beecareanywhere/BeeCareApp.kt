package com.beecareanywhere

import android.app.Application
import com.beecareanywhere.di.ServiceLocator

class BeeCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
