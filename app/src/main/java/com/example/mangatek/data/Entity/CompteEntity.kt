package com.example.mangatek.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "comptes")
data class CompteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String,  // Mot de passe hashé (ne jamais stocker en clair)
    val createdAt: Long = System.currentTimeMillis()
)

// Classe pour les relations Compte avec Favoris
data class CompteWithFavoris(
    @Embedded val compte: CompteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "compteId"  // Le champ dans FavorisEntity qui référence CompteEntity
    )
    val favoris: List<FavorisEntity>
)
