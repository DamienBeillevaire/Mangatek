package com.example.mangatek.data.repository

import com.example.mangatek.data.dao.CompteDao
import com.example.mangatek.data.entity.CompteEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

// =============================================================================
// REPOSITORY COMPTES — Gestion des comptes utilisateurs
//
// Responsabilités :
//   - Création de compte avec vérification d'unicité de l'email
//   - Authentification (login) avec comparaison de hash
//   - Hashage SHA-256 des mots de passe avant stockage
//
// SÉCURITÉ DES MOTS DE PASSE :
//   Les mots de passe ne sont JAMAIS stockés en clair.
//   On utilise SHA-256 (implémenté ici via MessageDigest de Java).
//   Pour une app de production : bcrypt ou Argon2 (plus résistants aux attaques).
//   SHA-256 est vulnérable aux rainbow tables sans sel (salt) → acceptable pour un projet étudiant.
// =============================================================================

class CompteRepository(private val compteDao: CompteDao) {

    /** Flow de tous les comptes (pour debug ou liste d'utilisateurs). */
    val allComptes: Flow<List<CompteEntity>> = compteDao.getAllComptes()

    /**
     * Crée un nouveau compte utilisateur.
     *
     * PROCESSUS :
     *  1. Vérifie que l'email n'est pas déjà utilisé (contrainte métier)
     *  2. Hash le mot de passe en SHA-256
     *  3. Insère le CompteEntity dans Room
     *  4. Retourne l'ID auto-généré (Long) ou null si email déjà pris
     *
     * @return ID du nouveau compte (Long) si succès, null si email déjà utilisé
     */
    suspend fun createCompte(username: String, email: String, password: String): Long? {
        // Vérification d'unicité avant insertion (évite l'exception SQLite UNIQUE constraint)
        if (compteDao.getCompteByEmail(email) != null) {
            return null   // Email déjà utilisé
        }

        val compte = CompteEntity(
            username = username,
            email = email,
            passwordHash = hashPassword(password)   // Toujours hasher avant stockage
        )

        return compteDao.insert(compte)   // Room retourne l'ID auto-généré (rowId)
    }

    /**
     * Authentifie un utilisateur par email et mot de passe.
     *
     * PROCESSUS :
     *  1. Cherche le compte par email (null si inexistant)
     *  2. Hash le mot de passe saisi et compare avec le hash stocké
     *  3. Retourne le compte si correspondance, null sinon
     *
     * @return CompteEntity si authentification réussie, null sinon
     */
    suspend fun login(email: String, password: String): CompteEntity? {
        val compte = compteDao.getCompteByEmail(email) ?: return null   // Email inconnu

        // Comparaison des hashes (jamais comparer les mots de passe en clair)
        return if (compte.passwordHash == hashPassword(password)) {
            compte   // Mot de passe correct → retourne le compte
        } else {
            null     // Mot de passe incorrect
        }
    }

    /** Récupère un compte par son ID (utilisé pour recharger la session). */
    suspend fun getCompteById(compteId: Int): CompteEntity? {
        return compteDao.getCompteById(compteId)
    }

    /** Supprime un compte et ses favoris en cascade (ForeignKey.CASCADE dans FavorisEntity). */
    suspend fun deleteCompte(compteId: Int) {
        compteDao.deleteCompte(compteId)
    }

    /**
     * Hash un mot de passe en SHA-256 et retourne le résultat en hexadécimal.
     *
     * FONCTIONNEMENT :
     *  1. password.toByteArray()     → convertit le String en octets (UTF-8)
     *  2. MessageDigest.digest()     → calcule le hash SHA-256 (32 octets)
     *  3. joinToString("%02x")       → convertit chaque octet en 2 caractères hex
     *     Ex: [0xAB, 0x0F] → "ab0f"
     *
     * Résultat : String de 64 caractères hexadécimaux.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
