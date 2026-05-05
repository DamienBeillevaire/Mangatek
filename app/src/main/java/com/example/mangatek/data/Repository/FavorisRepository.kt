package com.example.mangatek.data.repository

import com.example.mangatek.data.dao.FavorisDao
import com.example.mangatek.data.entity.FavorisEntity
import kotlinx.coroutines.flow.Flow

// =============================================================================
// REPOSITORY FAVORIS — Abstraction de la couche d'accès aux données locales (Room)
//
// Pattern Repository : le ViewModel ne connaît pas Room/DAO directement.
// Il passe par le Repository qui encapsule la logique d'accès aux données.
//
// FLUX DE DONNÉES :
//   FavorisViewModel → FavorisRepository → FavorisDao → Room (SQLite)
// =============================================================================

class FavorisRepository(private val favorisDao: FavorisDao) {

    /**
     * Retourne un Flow (observable) des favoris d'un compte donné.
     * Room émet automatiquement une nouvelle valeur à chaque modification
     * de la table "favoris" pour ce compteId → l'UI se met à jour en temps réel.
     *
     * Le Flow est "froid" : Room ne commence à observer qu'au moment où
     * quelqu'un collecte ce Flow (via stateIn dans FavorisViewModel).
     *
     * @param compteId ID du compte dont on veut les favoris
     */
    fun getFavorisByCompte(compteId: Int): Flow<List<FavorisEntity>> {
        return favorisDao.getFavorisByCompte(compteId)
    }

    /**
     * Ajoute un favori en base de données.
     * La clé primaire composite (compteId, malId) empêche les doublons.
     */
    suspend fun addFavori(favoris: FavorisEntity) {
        favorisDao.insert(favoris)
    }

    /**
     * Supprime un favori par ses identifiants (pas besoin de l'entité complète).
     * Utilise une requête SQL directe dans le DAO pour éviter une lecture préalable.
     */
    suspend fun deleteFavori(compteId: Int, malId: Int) {
        favorisDao.deleteFavori(compteId, malId)
    }

    /**
     * Vérifie si un anime est en favoris pour un compte donné.
     * @return true si le couple (compteId, malId) existe dans la BDD
     */
    suspend fun isFavorite(compteId: Int, malId: Int): Boolean {
        return favorisDao.isFavorite(compteId, malId)
    }

    /**
     * Toggle favori : ajoute si absent, supprime si présent.
     * La logique métier est ici plutôt que dans le DAO (qui ne fait que des opérations atomiques)
     * ou dans le ViewModel (qui ne devrait pas gérer la logique de persistance).
     *
     * NOTE : pas de transaction Room explicite — dans une app de production, on utiliserait
     * @Transaction pour garantir l'atomicité de la vérification + insertion/suppression.
     */
    suspend fun toggleFavori(favoris: FavorisEntity) {
        if (favorisDao.isFavorite(favoris.compteId, favoris.malId)) {
            favorisDao.deleteFavori(favoris.compteId, favoris.malId)   // Était favori → supprimer
        } else {
            favorisDao.insert(favoris)   // N'était pas favori → ajouter
        }
    }
}
