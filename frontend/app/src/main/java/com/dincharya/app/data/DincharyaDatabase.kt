package com.dincharya.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    entities = [TaskEntity::class, TaskEventEntity::class, SubtaskEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class DincharyaDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskEventDao(): TaskEventDao
    abstract fun subtaskDao(): SubtaskDao

    companion object {
        /**
         * v2 -> v3: tasks spawned by a recurring completion remember their
         * parent (undo support). Nullable, no default needed for old rows.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN spawnedBy INTEGER")
            }
        }

        /**
         * v1 -> v2: notes + repeat rules on tasks, and the subtasks table.
         * New task columns are nullable / defaulted so old rows stay valid.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS subtasks (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "taskId INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "isDone INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtasks_taskId ON subtasks(taskId)")
            }
        }
    }
}
