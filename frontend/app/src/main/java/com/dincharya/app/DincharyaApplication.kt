package com.dincharya.app

import android.app.Application
import com.dincharya.app.app.Graph

/**
 * Application entry point. Initialises the [Graph] (database, repository,
 * settings) once before any activity or receiver can touch them.
 */
class DincharyaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
