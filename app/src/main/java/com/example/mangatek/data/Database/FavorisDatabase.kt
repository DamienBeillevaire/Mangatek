package com.example.mangatek.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(
    entities = [FavorisEntity::class],
    version = 2,             // ← 1 → 2 car le schéma change
    exportSchema = false
)

abstract class FavorisManagerDatabase : RoomDatabase() {
    abstract fun favorisDao(): FavorisDao
    companion object {
        @Volatile
        private var INSTANCE: FavorisManagerDatabase? = null
        fun getDatabase(context: Context): FavorisManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FavorisManagerDatabase::class.java,
                    "favoris_manager_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}