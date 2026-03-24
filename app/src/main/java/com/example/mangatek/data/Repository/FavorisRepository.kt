package com.example.mangatek.data

import kotlinx.coroutines.flow.Flow
class FavorisRepository(private val favorisDao: FavorisDao) {
    val allFavoris: Flow<List<FavorisEntity>> = favorisDao.getAllFavoris()
    suspend fun insert(favoris: FavorisEntity) {
        favorisDao.insert(favoris)
    }
    suspend fun delete(favoris: FavorisEntity) {
        favorisDao.delete(favoris)
    }
}