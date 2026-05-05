package com.example.mangatek.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mangatek.data.dao.CompteDao
import com.example.mangatek.data.dao.FavorisDao
import com.example.mangatek.data.entity.CompteEntity
import com.example.mangatek.data.entity.FavorisEntity

@Database(
    entities = [CompteEntity::class, FavorisEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MangatekDatabase : RoomDatabase() {

    abstract fun compteDao(): CompteDao
    abstract fun favorisDao(): FavorisDao

    companion object {
        @Volatile
        private var INSTANCE: MangatekDatabase? = null

        fun getDatabase(context: Context): MangatekDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MangatekDatabase::class.java,
                    "mangatek_database"
                )
                    .fallbackToDestructiveMigration()  // Pour le développement
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
