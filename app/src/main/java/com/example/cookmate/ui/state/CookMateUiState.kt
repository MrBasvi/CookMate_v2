package com.example.cookmate.ui.state

import com.example.cookmate.data.model.Meal

sealed class MealUiState {
    data object Loading : MealUiState()
    data class Success(val meals: List<Meal>) : MealUiState()
    data class Error(val message: String) : MealUiState()
    data object Empty : MealUiState()
}

sealed class MealDetailUiState {
    data object Loading : MealDetailUiState()
    data class Success(val meal: Meal) : MealDetailUiState()
    data class Error(val message: String) : MealDetailUiState()
}

data class CookMateUiState(
    val searchQuery: String = "",
    val mealListState: MealUiState = MealUiState.Empty,
    val mealDetailState: MealDetailUiState? = null,
    val selectedMealId: String? = null,
    val favorites: List<String> = emptyList(),
    val favoriteMeals: List<Meal> = emptyList(),
    val allMeals: List<Meal> = emptyList()
)
