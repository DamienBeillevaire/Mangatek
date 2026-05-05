
package com.example.mangatek.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mangatek.data.entity.FavorisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavorisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoris: FavorisEntity)

    @Delete
    suspend fun delete(favoris: FavorisEntity)

    // Récupérer tous les favoris d'un compte spécifique
    @Query("SELECT * FROM favoris WHERE compteId = :compteId ORDER BY addedAt DESC")
    fun getFavorisByCompte(compteId: Int): Flow<List<FavorisEntity>>

    // Vérifier si un anime est favori pour un compte
    @Query("SELECT EXISTS(SELECT 1 FROM favoris WHERE compteId = :compteId AND malId = :malId)")
    suspend fun isFavorite(compteId: Int, malId: Int): Boolean

    // Supprimer un favori spécifique
    @Query("DELETE FROM favoris WHERE compteId = :compteId AND malId = :malId")
    suspend fun deleteFavori(compteId: Int, malId: Int)

    // Récupérer tous les favoris (pour debug)
    @Query("SELECT * FROM favoris")
    fun getAllFavoris(): Flow<List<FavorisEntity>>
}
