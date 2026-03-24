package com.example.mangatek.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoris")
data class FavorisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
)