package com.example.cookmate.navigation

sealed class RecipeRoutes {
    data object Search : RecipeRoutes()
    data class Detail(val mealId: String) : RecipeRoutes()
}
