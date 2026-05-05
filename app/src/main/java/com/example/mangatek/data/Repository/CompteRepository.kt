package com.example.mangatek.data.repository

import com.example.mangatek.data.dao.CompteDao
import com.example.mangatek.data.entity.CompteEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class CompteRepository(private val compteDao: CompteDao) {

    val allComptes: Flow<List<CompteEntity>> = compteDao.getAllComptes()

    /**
     * Créer un nouveau compte
     * @return ID du compte créé, ou null si l'email existe déjà
     */
    suspend fun createCompte(username: String, email: String, password: String): Long? {
        // Vérifier si l'email existe déjà
        if (compteDao.getCompteByEmail(email) != null) {
            return null  // Email déjà utilisé
        }

        val compte = CompteEntity(
            username = username,
            email = email,
            passwordHash = hashPassword(password)
        )

        return compteDao.insert(compte)
    }

    /**
     * Connexion avec email et mot de passe
     * @return CompteEntity si les identifiants sont corrects, null sinon
     */
    suspend fun login(email: String, password: String): CompteEntity? {
        val compte = compteDao.getCompteByEmail(email) ?: return null

        // Vérifier le mot de passe
        return if (compte.passwordHash == hashPassword(password)) {
            compte
        } else {
            null
        }
    }

    suspend fun getCompteById(compteId: Int): CompteEntity? {
        return compteDao.getCompteById(compteId)
    }

    suspend fun deleteCompte(compteId: Int) {
        compteDao.deleteCompte(compteId)
    }

    /**
     * Hash simple du mot de passe (SHA-256)
     * Note: Pour une vraitable app de production, utilisez bcrypt ou argon2
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}