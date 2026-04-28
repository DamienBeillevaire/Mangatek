package com.example.mangatek.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoris")
data class FavorisEntity(
    @PrimaryKey
    val malId: Int,          // ID unique de l'anime (remplace autoGenerate)
    val title: String,
    val imageUrl: String,
    val score: Double?,
    val addedAt: String      // date d'ajout
)