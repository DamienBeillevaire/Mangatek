package com.example.mangatek.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mangatek.data.dao.CompteDao
import com.example.mangatek.data.dao.FavorisDao
import com.example.mangatek.data.entity.CompteEntity
import com.example.mangatek.data.entity.FavorisEntity

// =============================================================================
// BASE DE DONNÉES ROOM — Configuration SQLite locale
//
// Room est la couche d'abstraction Android au-dessus de SQLite.
// Elle génère automatiquement le code SQL à partir des annotations.
//
// ARCHITECTURE :
//   MangatekDatabase (RoomDatabase)
//     ├── CompteEntity  → table "comptes"
//     └── FavorisEntity → table "favoris" (FK sur comptes.id)
//
// SINGLETON : une seule instance par processus Android, protégée par @Volatile + synchronized
// pour la thread-safety lors de la création concurrente.
// =============================================================================

@Database(
    entities = [CompteEntity::class, FavorisEntity::class],  // Tables de la BDD
    version = 1,           // Version du schéma — incrémenter lors de migrations
    exportSchema = false   // Ne pas exporter le schéma JSON (utile pour les tests de migration)
)
abstract class MangatekDatabase : RoomDatabase() {

    /** Accès au DAO des comptes → opérations CRUD sur la table "comptes". */
    abstract fun compteDao(): CompteDao

    /** Accès au DAO des favoris → opérations CRUD sur la table "favoris". */
    abstract fun favorisDao(): FavorisDao

    companion object {
        /**
         * @Volatile : assure que la valeur d'INSTANCE est toujours lue depuis la mémoire principale
         * (pas depuis un cache CPU local) → garantit la visibilité entre threads.
         */
        @Volatile
        private var INSTANCE: MangatekDatabase? = null

        /**
         * Retourne l'instance unique de la base de données (pattern Singleton thread-safe).
         *
         * Double-checked locking :
         *  1. Vérifie INSTANCE sans lock (fast path si déjà créé)
         *  2. Si null, prend le lock et vérifie à nouveau (un autre thread a pu créer l'instance)
         *  3. Si toujours null, crée l'instance
         *
         * @param context ApplicationContext recommandé (évite les fuites mémoire d'Activity)
         */
        fun getDatabase(context: Context): MangatekDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MangatekDatabase::class.java,
                    "mangatek_database"   // Nom du fichier SQLite sur le disque
                )
                    // En développement : détruit et recrée la BDD si la version change.
                    // En production : utiliser addMigrations() pour préserver les données.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
