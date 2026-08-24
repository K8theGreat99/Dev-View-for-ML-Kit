package com.k8thegreat.devview.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SampleEntity::class], version = 1, exportSchema = false)
abstract class DevViewDatabase : RoomDatabase() {

    abstract fun sampleDao(): SampleDao

    companion object {
        @Volatile
        private var instance: DevViewDatabase? = null

        fun get(context: Context): DevViewDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DevViewDatabase::class.java,
                    "dev-view.db",
                ).build().also { instance = it }
            }
    }
}
