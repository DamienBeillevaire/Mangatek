package com.example.mangatek.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.mangatek.data.entity.CompteEntity
import com.example.mangatek.data.entity.CompteWithFavoris
import kotlinx.coroutines.flow.Flow

// =============================================================================
// DAO COMPTES — Data Access Object pour la table "comptes"
// =============================================================================

@Dao
interface CompteDao {

    /**
     * Insère un nouveau compte.
     * OnConflictStrategy.ABORT : lève une exception si la clé primaire est en conflit
     * (normalement impossible avec autoGenerate = true, mais protège contre les appels multiples).
     *
     * @return L'ID (rowId) du compte inséré — utilisé pour recharger le compte après création.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(compte: CompteEntity): Long

    /**
     * Recherche un compte par email (utilisé pour la connexion et la vérification d'unicité).
     * LIMIT 1 : optimisation SQL — on s'arrête dès qu'on trouve un résultat.
     *
     * @return CompteEntity si trouvé, null sinon
     */
    @Query("SELECT * FROM comptes WHERE email = :email LIMIT 1")
    suspend fun getCompteByEmail(email: String): CompteEntity?

    /**
     * Recherche un compte par son ID (utilisé pour recharger la session depuis SharedPreferences).
     *
     * @return CompteEntity si trouvé, null si l'ID n'existe pas en BDD
     */
    @Query("SELECT * FROM comptes WHERE id = :compteId")
    suspend fun getCompteById(compteId: Int): CompteEntity?

    /** Flow de tous les comptes (pour debug). Room émet une nouvelle liste à chaque changement. */
    @Query("SELECT * FROM comptes")
    fun getAllComptes(): Flow<List<CompteEntity>>

    /** Supprime un compte par son ID. Les favoris associés sont supprimés en cascade (FK CASCADE). */
    @Query("DELETE FROM comptes WHERE id = :compteId")
    suspend fun deleteCompte(compteId: Int)

    /**
     * Récupère un compte avec tous ses favoris (requête JOIN via @Relation).
     *
     * @Transaction : garantit que les deux requêtes (compte + favoris) s'exécutent
     * dans la même transaction SQLite → données cohérentes même en cas de modification concurrente.
     *
     * Retourne un Flow → Room réémet à chaque modification du compte ou de ses favoris.
     */
    @Transaction
    @Query("SELECT * FROM comptes WHERE id = :compteId")
    fun getCompteWithFavoris(compteId: Int): Flow<CompteWithFavoris?>
}
