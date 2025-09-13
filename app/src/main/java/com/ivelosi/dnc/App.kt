package com.ivelosi.dnc

import android.app.Application
import com.ivelosi.dnc.data.AppContainer
import com.ivelosi.dnc.data.AppDataContainer

class App : Application() {
    lateinit var container: AppContainer

    fun initializeContainer(activity: MainActivity) {
        container = AppDataContainer(activity)
    }
}
