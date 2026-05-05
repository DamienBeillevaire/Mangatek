package com.example.mangatek.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

// =============================================================================
// ENTITÉ ROOM — Table "comptes" (utilisateurs de l'application)
//
// @Entity : indique à Room que cette classe correspond à une table SQLite.
// Room génère automatiquement le CREATE TABLE correspondant.
//
// SCHÉMA SQL généré :
//   CREATE TABLE comptes (
//     id INTEGER PRIMARY KEY AUTOINCREMENT,
//     username TEXT NOT NULL,
//     email TEXT NOT NULL,
//     passwordHash TEXT NOT NULL,
//     createdAt INTEGER NOT NULL
//   )
// =============================================================================

@Entity(tableName = "comptes")
data class CompteEntity(

    /** Clé primaire auto-générée par SQLite (AUTOINCREMENT). Défaut 0 = pas encore inséré. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Nom d'affiché sur le profil (n'a pas besoin d'être unique). */
    val username: String,

    /** Email utilisé pour l'authentification (unique enforced par la logique du Repository). */
    val email: String,

    /**
     * Hash SHA-256 du mot de passe (64 caractères hexadécimaux).
     * JAMAIS stocker le mot de passe en clair.
     * La conversion clair → hash est faite dans CompteRepository.hashPassword().
     */
    val passwordHash: String,

    /**
     * Timestamp de création du compte (millisecondes depuis epoch Unix).
     * Valeur par défaut : maintenant au moment de l'instanciation.
     * Affiché sur l'écran Profil après formatage : SimpleDateFormat("dd/MM/yyyy").
     */
    val createdAt: Long = System.currentTimeMillis()
)

// =============================================================================
// RELATION — CompteEntity avec ses FavorisEntity associés
// Utilisé pour des requêtes JOIN via @Transaction dans CompteDao.
// =============================================================================

/**
 * Classe de résultat pour la requête "compte + tous ses favoris".
 *
 * @Embedded   : inclut tous les champs de CompteEntity dans la requête
 * @Relation   : Room exécute automatiquement une sous-requête pour récupérer
 *               les FavorisEntity dont le champ "compteId" correspond au "id" du compte.
 *
 * Utilisé avec @Transaction dans CompteDao.getCompteWithFavoris().
 * Non utilisé dans l'UI actuelle mais disponible pour une future évolution.
 */
data class CompteWithFavoris(
    @Embedded val compte: CompteEntity,
    @Relation(
        parentColumn = "id",          // Colonne référencée dans CompteEntity
        entityColumn = "compteId"     // Colonne de référence dans FavorisEntity
    )
    val favoris: List<FavorisEntity>
)
