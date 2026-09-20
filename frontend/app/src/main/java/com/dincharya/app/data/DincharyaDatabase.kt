package com.dincharya.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The single local database of Dincharya.
 *
 * Local-first by design: this database is the source of truth for the whole
 * app. A later backend will only ever hold an encrypted mirror of it.
 *
 * Schemas are exported to app/schemas (see KSP config in build.gradle.kts)
 * so that any future schema change can be reviewed as a migration.
 */
@Database(
    entities = [TaskEntity::class, TaskEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DincharyaDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskEventDao(): TaskEventDao
}
