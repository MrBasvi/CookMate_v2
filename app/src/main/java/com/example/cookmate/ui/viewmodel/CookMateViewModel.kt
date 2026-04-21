package com.example.cookmate.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import com.example.cookmate.ui.state.CookMateUiState
import com.example.cookmate.ui.state.MealDetailUiState
import com.example.cookmate.ui.state.MealUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CookMateViewModel @Inject constructor(
    private val repository: MealRepository,
    private val favouriteService: FavouriteMealService
) : ViewModel() {
    var uiState by mutableStateOf(CookMateUiState())
        private set

    init {
        observeFavourites()
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            favouriteService.getAllFavourites().collectLatest { favouriteMeals ->
                val currentMeals = uiState.allMeals
                val mergedMeals = (currentMeals + favouriteMeals).distinctBy { it.idMeal }
                val favouriteIds = favouriteMeals.map { it.idMeal }

                uiState = uiState.copy(
                    favorites = favouriteIds,
                    allMeals = mergedMeals,
                    favoriteMeals = favouriteMeals
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    fun searchMeals(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            uiState = uiState.copy(mealListState = MealUiState.Empty)
            return
        }

        uiState = uiState.copy(mealListState = MealUiState.Loading)

        viewModelScope.launch {
            try {
                val meals = repository.searchMealsByName(trimmedQuery)
                
                if (meals.isEmpty()) {
                    uiState = uiState.copy(mealListState = MealUiState.Empty)
                } else {
                    uiState = uiState.copy(
                        mealListState = MealUiState.Success(meals),
                        allMeals = (uiState.allMeals + meals).distinctBy { it.idMeal }
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    mealListState = MealUiState.Error("Ошибка поиска: ${e.localizedMessage ?: "Проверьте интернет"}")
                )
            }
        }
    }

    fun getMealDetails(mealId: String) {
        uiState = uiState.copy(
            selectedMealId = mealId,
            mealDetailState = MealDetailUiState.Loading
        )

        viewModelScope.launch {
            try {
                val favouriteMeal = getStoredFavouriteMeal(mealId)
                if (favouriteMeal?.ingredients?.isNotEmpty() == true) {
                    uiState = uiState.copy(
                        mealDetailState = MealDetailUiState.Success(favouriteMeal),
                        allMeals = (uiState.allMeals + favouriteMeal).distinctBy { it.idMeal }
                    )
                    return@launch
                }

                val meal = repository.getMealDetails(mealId)
                uiState = uiState.copy(
                    mealDetailState = MealDetailUiState.Success(meal),
                    allMeals = (uiState.allMeals + meal).distinctBy { it.idMeal }
                )
            } catch (e: Exception) {
                val cachedMeal = getStoredFavouriteMeal(mealId)
                    ?: uiState.allMeals.firstOrNull { it.idMeal == mealId }
                if (cachedMeal != null) {
                    uiState = uiState.copy(
                        mealDetailState = MealDetailUiState.Success(cachedMeal),
                        allMeals = (uiState.allMeals + cachedMeal).distinctBy { it.idMeal }
                    )
                } else {
                    uiState = uiState.copy(
                        mealDetailState = MealDetailUiState.Error("Ошибка загрузки: ${e.localizedMessage ?: "Проверьте интернет"}")
                    )
                }
            }
        }
    }

    private suspend fun getStoredFavouriteMeal(mealId: String): Meal? =
        favouriteService.getFavourite(mealId)
            ?: uiState.favoriteMeals.firstOrNull { it.idMeal == mealId }



    fun toggleFavorite(mealId: String) {
        viewModelScope.launch {
            val mealFromList = uiState.allMeals.firstOrNull { it.idMeal == mealId }
            val mealFromDetail = (uiState.mealDetailState as? MealDetailUiState.Success)?.meal
            val meal = when {
                mealFromList != null -> mealFromList
                mealFromDetail?.idMeal == mealId -> mealFromDetail
                else -> null
            }

            if (meal == null) {
                return@launch
            }

            val isAlreadyFavorite = mealId in uiState.favorites
            try {
                if (isAlreadyFavorite) {
                    favouriteService.removeFavourite(mealId)
                } else {
                    val mealWithDetails = if (meal.ingredients.isEmpty()) {
                        repository.getMealDetails(mealId)
                    } else {
                        meal
                    }
                    favouriteService.addFavourite(mealWithDetails)
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    mealDetailState = MealDetailUiState.Error("Ошибка избранного: ${e.localizedMessage ?: "Попробуйте позже"}")
                )
            }
        }
    }

    fun clearDetail() {
        uiState = uiState.copy(
            selectedMealId = null,
            mealDetailState = null
        )
    }
}
