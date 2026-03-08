package com.example.cookmate.data.model

data class Meal(
    val idMeal: String,
    val strMeal: String,
    val strCategory: String,
    val strArea: String,
    val strInstructions: String,
    val strMealThumb: String,
    val ingredients: List<Ingredient> = emptyList()
)

data class Ingredient(
    val name: String,
    val measure: String
)
