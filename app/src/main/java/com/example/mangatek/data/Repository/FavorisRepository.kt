package com.example.mangatek.data.repository

import com.example.mangatek.data.dao.FavorisDao
import com.example.mangatek.data.entity.FavorisEntity
import kotlinx.coroutines.flow.Flow

class FavorisRepository(private val favorisDao: FavorisDao) {

    /**
     * Récupérer tous les favoris d'un compte spécifique
     */
    fun getFavorisByCompte(compteId: Int): Flow<List<FavorisEntity>> {
        return favorisDao.getFavorisByCompte(compteId)
    }

    /**
     * Ajouter un favori pour un compte
     */
    suspend fun addFavori(favoris: FavorisEntity) {
        favorisDao.insert(favoris)
    }

    /**
     * Supprimer un favori
     */
    suspend fun deleteFavori(compteId: Int, malId: Int) {
        favorisDao.deleteFavori(compteId, malId)
    }

    /**
     * Vérifier si un anime est déjà en favori pour un compte
     */
    suspend fun isFavorite(compteId: Int, malId: Int): Boolean {
        return favorisDao.isFavorite(compteId, malId)
    }

    /**
     * Toggle favori (ajouter si absent, supprimer si présent)
     */
    suspend fun toggleFavori(favoris: FavorisEntity) {
        if (favorisDao.isFavorite(favoris.compteId, favoris.malId)) {
            favorisDao.deleteFavori(favoris.compteId, favoris.malId)
        } else {
            favorisDao.insert(favoris)
        }
    }
}