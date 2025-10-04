package com.ivelosi.dnc

import android.app.Application


class App : Application() {
    lateinit var container: AppContainer

    fun initializeContainer(activity: MainActivity) {
        container = AppDataContainer(activity)
    }
}
