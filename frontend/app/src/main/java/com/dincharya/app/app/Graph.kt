package com.dincharya.app.app

import android.content.Context
import androidx.room.Room
import com.dincharya.app.data.DincharyaDatabase
import com.dincharya.app.data.TaskRepository

/**
 * Tiny hand-rolled service locator.
 *
 * v1 has exactly one database and one repository; a DI framework would add
 * build complexity without value. If the app grows to need scoped
 * dependencies (e.g. per-account after auth lands), replace this with Hilt —
 * the call sites all go through `Graph.x`, so the migration is mechanical.
 */
object Graph {

    @Volatile
    private var initialized = false

    lateinit var database: DincharyaDatabase
        private set

    lateinit var repository: TaskRepository
        private set

    lateinit var settings: SettingsStore
        private set

    /** Idempotent; called from DincharyaApplication.onCreate(). */
    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            database = Room.databaseBuilder(
                appContext,
                DincharyaDatabase::class.java,
                "dincharya.db",
            ).build()
            settings = SettingsStore(appContext)
            repository = TaskRepository(database.taskDao(), database.taskEventDao())
            initialized = true
        }
    }
}
