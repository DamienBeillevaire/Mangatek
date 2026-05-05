package com.example.mangatek.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mangatek.data.entity.FavorisEntity
import kotlinx.coroutines.flow.Flow

// =============================================================================
// DAO FAVORIS — Data Access Object pour la table "favoris"
//
// @Dao : interface annotée dont Room génère l'implémentation concrète à la compilation.
// Chaque méthode devient une opération SQLite.
//
// TYPES DE RETOUR :
//   - suspend fun  : opération ponctuelle (insert, delete, query unique)
//   - Flow<T>      : opération réactive — Room réémet automatiquement quand les données changent
// =============================================================================

@Dao
interface FavorisDao {

    /**
     * Insère un favori dans la table.
     * OnConflictStrategy.REPLACE : si la clé composite (compteId, malId) existe déjà,
     * la ligne existante est remplacée → comportement "upsert".
     * Note : dans l'app actuelle, on vérifie d'abord avec isFavorite() → ce cas ne devrait pas arriver.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoris: FavorisEntity)

    /**
     * Supprime un favori en passant l'entité complète.
     * Room utilise la clé primaire (compteId, malId) pour trouver et supprimer la ligne.
     * Non utilisé dans l'app actuelle (on préfère deleteFavori par ID pour ne pas charger l'entité).
     */
    @Delete
    suspend fun delete(favoris: FavorisEntity)

    /**
     * Retourne un Flow des favoris d'un compte, triés du plus récent au plus ancien.
     * Room observe la table "favoris" et réémet la liste à chaque modification.
     *
     * SQL : SELECT * FROM favoris WHERE compteId = :compteId ORDER BY addedAt DESC
     */
    @Query("SELECT * FROM favoris WHERE compteId = :compteId ORDER BY addedAt DESC")
    fun getFavorisByCompte(compteId: Int): Flow<List<FavorisEntity>>

    /**
     * Vérifie si un anime est en favoris pour un compte donné.
     * EXISTS retourne 1 (true) ou 0 (false) → Room le convertit en Boolean.
     *
     * SQL : SELECT EXISTS(SELECT 1 FROM favoris WHERE compteId = ? AND malId = ?)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favoris WHERE compteId = :compteId AND malId = :malId)")
    suspend fun isFavorite(compteId: Int, malId: Int): Boolean

    /**
     * Supprime un favori par ses identifiants (sans charger l'entité complète).
     * Plus efficace que @Delete quand on n'a pas l'entité en mémoire.
     */
    @Query("DELETE FROM favoris WHERE compteId = :compteId AND malId = :malId")
    suspend fun deleteFavori(compteId: Int, malId: Int)

    /** Retourne tous les favoris de tous les comptes (utilisé pour le debug). */
    @Query("SELECT * FROM favoris")
    fun getAllFavoris(): Flow<List<FavorisEntity>>
}
