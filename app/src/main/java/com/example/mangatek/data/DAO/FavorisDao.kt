package com.example.mangatek.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface FavorisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoris: FavorisEntity)
    @Delete
    suspend fun delete(favoris: FavorisEntity)
    @Query("SELECT * FROM favoris")
    fun getAllFavoris(): Flow<List<FavorisEntity>>
}