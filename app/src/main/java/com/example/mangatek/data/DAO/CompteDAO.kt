package com.example.mangatek.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.mangatek.data.entity.CompteEntity
import com.example.mangatek.data.entity.CompteWithFavoris
import kotlinx.coroutines.flow.Flow

@Dao
interface CompteDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(compte: CompteEntity): Long  // Retourne l'ID du compte créé

    @Query("SELECT * FROM comptes WHERE email = :email LIMIT 1")
    suspend fun getCompteByEmail(email: String): CompteEntity?

    @Query("SELECT * FROM comptes WHERE id = :compteId")
    suspend fun getCompteById(compteId: Int): CompteEntity?

    @Query("SELECT * FROM comptes")
    fun getAllComptes(): Flow<List<CompteEntity>>

    @Query("DELETE FROM comptes WHERE id = :compteId")
    suspend fun deleteCompte(compteId: Int)

    // Récupérer un compte avec tous ses favoris
    @Transaction
    @Query("SELECT * FROM comptes WHERE id = :compteId")
    fun getCompteWithFavoris(compteId: Int): Flow<CompteWithFavoris?>
}
