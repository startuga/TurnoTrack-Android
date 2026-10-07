package com.startuga.turnotrack.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkEntryEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workEntryDao(): WorkEntryDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "turnotrack.db").build()
    }
}
