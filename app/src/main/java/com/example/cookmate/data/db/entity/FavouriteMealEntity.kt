package com.example.cookmate.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cookmate.data.model.Ingredient

@Entity(tableName = "favourite_meals")
data class FavouriteMealEntity(
    @PrimaryKey
    val idMeal: String,
    val strMeal: String,
    val strCategory: String,
    val strArea: String,
    val strInstructions: String,
    val strMealThumb: String,
    val ingredients: List<Ingredient> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
)
