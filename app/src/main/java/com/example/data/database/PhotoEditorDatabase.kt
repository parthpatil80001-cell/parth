package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EditProject::class], version = 1, exportSchema = false)
abstract class PhotoEditorDatabase : RoomDatabase() {
    abstract fun editProjectDao(): EditProjectDao

    companion object {
        @Volatile
        private var INSTANCE: PhotoEditorDatabase? = null

        fun getDatabase(context: Context): PhotoEditorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhotoEditorDatabase::class.java,
                    "photo_editor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
