package com.example.mangatek.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// =============================================================================
// ENTITÉ ROOM — Table "favoris" (animes favoris par utilisateur)
//
// PARTICULARITÉS :
//   1. CLÉ PRIMAIRE COMPOSITE (compteId, malId) : un même anime peut être favori
//      de plusieurs utilisateurs, mais un utilisateur ne peut avoir le même
//      anime qu'une seule fois. → évite les doublons sans colonne "id" séparée.
//
//   2. CLÉ ÉTRANGÈRE (FK) sur compteId → comptes.id :
//      - Garantit l'intégrité référentielle (pas de favoris sans compte)
//      - CASCADE DELETE : si le compte est supprimé, ses favoris le sont aussi
//
//   3. INDEX sur compteId : accélère les requêtes WHERE compteId = ?
//      (utilisé à chaque fois qu'on charge les favoris d'un utilisateur)
//
// SCHÉMA SQL généré :
//   CREATE TABLE favoris (
//     compteId INTEGER NOT NULL,
//     malId INTEGER NOT NULL,
//     title TEXT NOT NULL,
//     imageUrl TEXT NOT NULL,
//     score REAL,
//     addedAt INTEGER NOT NULL,
//     PRIMARY KEY (compteId, malId),
//     FOREIGN KEY (compteId) REFERENCES comptes(id) ON DELETE CASCADE
//   )
// =============================================================================

@Entity(
    tableName = "favoris",
    primaryKeys = ["compteId", "malId"],   // Clé composite → un anime unique par compte
    foreignKeys = [
        ForeignKey(
            entity = CompteEntity::class,
            parentColumns = ["id"],          // Colonne dans la table "comptes"
            childColumns = ["compteId"],     // Colonne dans la table "favoris"
            onDelete = ForeignKey.CASCADE    // Suppression en cascade si le compte est supprimé
        )
    ],
    indices = [Index(value = ["compteId"])]  // Index sur compteId pour les requêtes par utilisateur
)
data class FavorisEntity(
    val compteId: Int,      // FK vers comptes.id — scope les favoris au bon utilisateur
    val malId: Int,          // ID unique MyAnimeList — partie de la clé primaire composite
    val title: String,       // Titre affiché (EN ou JP) — copié au moment de l'ajout en favori
    val imageUrl: String,    // URL de l'image — stockée pour l'affichage hors-ligne
    val score: Double?,      // Note /10 (nullable car certains animes n'ont pas de note)
    val addedAt: Long = System.currentTimeMillis()  // Pour tri par date d'ajout (ORDER BY addedAt DESC)
)
