package com.example.mangatek.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mangatek.data.entity.CompteEntity
import com.example.mangatek.data.repository.CompteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val currentCompte: CompteEntity? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val compteRepository: CompteRepository,
    private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // SharedPreferences pour garder l'utilisateur connecté
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    init {
        // Vérifier si un utilisateur est déjà connecté
        checkSavedSession()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            val savedCompteId = prefs.getInt("current_compte_id", -1)
            if (savedCompteId != -1) {
                compteRepository.getCompteById(savedCompteId)?.let { compte ->
                    _authState.value = AuthState(
                        currentCompte = compte,
                        isLoggedIn = true
                    )
                }
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            when {
                username.isBlank() -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Le nom d'utilisateur est requis"
                    )
                }
                email.isBlank() || !email.contains("@") -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Email invalide"
                    )
                }
                password.length < 6 -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Le mot de passe doit contenir au moins 6 caractères"
                    )
                }
                else -> {
                    val compteId = compteRepository.createCompte(username, email, password)

                    if (compteId != null) {
                        val compte = compteRepository.getCompteById(compteId.toInt())
                        saveSession(compteId.toInt())
                        _authState.value = AuthState(
                            isLoading = false,
                            currentCompte = compte,
                            isLoggedIn = true
                        )
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = "Cet email est déjà utilisé"
                        )
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val compte = compteRepository.login(email, password)

            if (compte != null) {
                saveSession(compte.id)
                _authState.value = AuthState(
                    isLoading = false,
                    currentCompte = compte,
                    isLoggedIn = true
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Email ou mot de passe incorrect"
                )
            }
        }
    }

    fun signOut() {
        clearSession()
        _authState.value = AuthState(isLoggedIn = false)
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    private fun saveSession(compteId: Int) {
        prefs.edit().putInt("current_compte_id", compteId).apply()
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }
}

class AuthViewModelFactory(
    private val compteRepository: CompteRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(compteRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}