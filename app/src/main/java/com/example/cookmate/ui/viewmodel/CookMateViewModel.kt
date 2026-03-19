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
        if (query.isBlank()) {
            uiState = uiState.copy(mealListState = MealUiState.Empty)
            return
        }

        uiState = uiState.copy(mealListState = MealUiState.Loading)

        viewModelScope.launch {
            try {
                val meals = repository.searchMealsByName(query)
                
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
                // Сначала проверяем, есть ли рецепт в избранном (там полные данные)
                val favouriteMeal = uiState.favoriteMeals.firstOrNull {
                    it.idMeal == mealId &&
                    it.ingredients.isNotEmpty()
                }

                if (favouriteMeal != null) {
                    uiState = uiState.copy(
                        mealDetailState = MealDetailUiState.Success(favouriteMeal),
                        allMeals = (uiState.allMeals + favouriteMeal).distinctBy { it.idMeal }
                    )
                    return@launch
                }

                val meal = repository.getMealDetails(mealId)
                if (mealId in uiState.favorites) {
                    runCatching { favouriteService.addFavourite(meal) }
                }
                
                uiState = uiState.copy(
                    mealDetailState = MealDetailUiState.Success(meal),
                    allMeals = (uiState.allMeals + meal).distinctBy { it.idMeal }
                )
            } catch (e: Exception) {
                // Если не удалось загрузить через API, пробуем взять из allMeals без полных данных
                val cachedMeal = uiState.allMeals.firstOrNull { it.idMeal == mealId }
                if (cachedMeal != null) {
                    uiState = uiState.copy(
                        mealDetailState = MealDetailUiState.Success(cachedMeal)
                    )
                } else {
                    uiState = uiState.copy(
                        mealDetailState = MealDetailUiState.Error("Ошибка загрузки: ${e.localizedMessage ?: "Проверьте интернет"}")
                    )
                }
            }
        }
    }



    fun toggleFavorite(meal: Meal) {
        viewModelScope.launch {
            try {
                val currentFavorites = uiState.favorites.toMutableList()
                val isAlreadyFavorite = meal.idMeal in currentFavorites

                if (isAlreadyFavorite) {
                    currentFavorites.remove(meal.idMeal)
                } else {
                    currentFavorites.add(meal.idMeal)
                }

                uiState = uiState.copy(favorites = currentFavorites)

                if (isAlreadyFavorite) {
                    favouriteService.removeFavourite(meal.idMeal)
                } else {
                    val mealWithDetails = if (meal.ingredients.isEmpty()) {
                        try {
                            repository.getMealDetails(meal.idMeal)
                        } catch (_: Exception) {
                            meal
                        }
                    } else {
                        meal
                    }

                    favouriteService.addFavourite(mealWithDetails)
                }
            } catch (e: Exception) {
                val rollbackFavorites = uiState.favorites.toMutableList()
                if (meal.idMeal in rollbackFavorites) {
                    rollbackFavorites.remove(meal.idMeal)
                } else {
                    rollbackFavorites.add(meal.idMeal)
                }
                uiState = uiState.copy(favorites = rollbackFavorites)
            }
        }
    }

    fun toggleFavorite(mealId: String) {
        viewModelScope.launch {
            try {
                val mealFromList = uiState.allMeals.firstOrNull { it.idMeal == mealId }
                val mealFromDetail = (uiState.mealDetailState as? MealDetailUiState.Success)?.meal
                val meal = when {
                    mealFromList != null -> mealFromList
                    mealFromDetail?.idMeal == mealId -> mealFromDetail
                    else -> null
                }

                if (meal != null) {
                    toggleFavorite(meal)
                    return@launch
                }

                val currentFavorites = uiState.favorites.toMutableList()
                if (mealId in currentFavorites) {
                    favouriteService.removeFavourite(mealId)
                    currentFavorites.remove(mealId)
                }
                uiState = uiState.copy(favorites = currentFavorites)
            } catch (_: Exception) {
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
