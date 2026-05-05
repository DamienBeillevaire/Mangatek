package com.example.mangatek.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "favoris",
    primaryKeys = ["compteId", "malId"],  // Clé composite : un anime par compte
    foreignKeys = [
        ForeignKey(
            entity = CompteEntity::class,
            parentColumns = ["id"],
            childColumns = ["compteId"],
            onDelete = ForeignKey.CASCADE  // Si on supprime un compte, ses favoris sont supprimés
        )
    ],
    indices = [Index(value = ["compteId"])]  // Index pour optimiser les requêtes
)
data class FavorisEntity(
    val compteId: Int,        // ID du compte propriétaire ← NOUVEAU
    val malId: Int,           // ID unique de l'anime
    val title: String,
    val imageUrl: String,
    val score: Double?,
    val addedAt: Long = System.currentTimeMillis()  // Timestamp au lieu de String
)
