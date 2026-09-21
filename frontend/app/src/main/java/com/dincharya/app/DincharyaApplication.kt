package com.dincharya.app

import android.app.Application
import com.dincharya.app.app.Graph
import com.dincharya.app.notifications.DailyBriefWorker

/**
 * Application entry point. Initialises the [Graph] (database, repository,
 * settings) once before any activity or receiver can touch them, and books
 * the two daily check-in notifications (morning brief, evening review).
 */
class DincharyaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        DailyBriefWorker.scheduleDailyBriefs(this)
    }
}
