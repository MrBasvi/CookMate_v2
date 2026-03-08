package com.example.cookmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import com.example.cookmate.ui.state.CookMateUiState
import com.example.cookmate.ui.state.MealDetailUiState
import com.example.cookmate.ui.state.MealUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CookMateViewModel @Inject constructor(
    private val repository: MealRepository,
    private val favouriteService: FavouriteMealService
) : ViewModel() {
    private val _uiState = MutableStateFlow(CookMateUiState())
    val uiState: StateFlow<CookMateUiState> = _uiState.asStateFlow()

    init {
        observeFavourites()
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            favouriteService.getAllFavourites().collectLatest { favouriteMeals ->
                val currentMeals = _uiState.value.allMeals
                val mergedMeals = (currentMeals + favouriteMeals).distinctBy { it.idMeal }
                val favouriteIds = favouriteMeals.map { it.idMeal }

                _uiState.value = _uiState.value.copy(
                    favorites = favouriteIds,
                    allMeals = mergedMeals
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun searchMeals(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(mealListState = MealUiState.Empty)
            return
        }

        _uiState.value = _uiState.value.copy(mealListState = MealUiState.Loading)

        viewModelScope.launch {
            try {
                val meals = repository.searchMealsByName(query)
                
                if (meals.isEmpty()) {
                    _uiState.value = _uiState.value.copy(mealListState = MealUiState.Empty)
                } else {
                    _uiState.value = _uiState.value.copy(
                        mealListState = MealUiState.Success(meals),
                        allMeals = (_uiState.value.allMeals + meals).distinctBy { it.idMeal }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    mealListState = MealUiState.Error("Ошибка поиска: ${e.localizedMessage ?: "Проверьте интернет"}")
                )
            }
        }
    }

    fun getMealDetails(mealId: String) {
        _uiState.value = _uiState.value.copy(
            selectedMealId = mealId,
            mealDetailState = MealDetailUiState.Loading
        )

        viewModelScope.launch {
            try {
                // Сначала проверяем, есть ли рецепт в избранном (там полные данные)
                val favouriteMeal = _uiState.value.allMeals.firstOrNull {
                    it.idMeal == mealId &&
                    it.idMeal in _uiState.value.favorites &&
                    it.ingredients.isNotEmpty()
                }

                if (favouriteMeal != null) {
                    _uiState.value = _uiState.value.copy(
                        mealDetailState = MealDetailUiState.Success(favouriteMeal),
                        allMeals = (_uiState.value.allMeals + favouriteMeal).distinctBy { it.idMeal }
                    )
                    return@launch
                }

                val meal = repository.getMealDetails(mealId)
                if (mealId in _uiState.value.favorites) {
                    runCatching { favouriteService.addFavourite(meal) }
                }
                
                _uiState.value = _uiState.value.copy(
                    mealDetailState = MealDetailUiState.Success(meal),
                    allMeals = (_uiState.value.allMeals + meal).distinctBy { it.idMeal }
                )
            } catch (e: Exception) {
                // Если не удалось загрузить через API, пробуем взять из allMeals без полных данных
                val cachedMeal = _uiState.value.allMeals.firstOrNull { it.idMeal == mealId }
                if (cachedMeal != null) {
                    _uiState.value = _uiState.value.copy(
                        mealDetailState = MealDetailUiState.Success(cachedMeal)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        mealDetailState = MealDetailUiState.Error("Ошибка загрузки: ${e.localizedMessage ?: "Проверьте интернет"}")
                    )
                }
            }
        }
    }



    fun toggleFavorite(meal: Meal) {
        viewModelScope.launch {
            try {
                val currentFavorites = _uiState.value.favorites.toMutableList()
                val isAlreadyFavorite = meal.idMeal in currentFavorites

                if (isAlreadyFavorite) {
                    currentFavorites.remove(meal.idMeal)
                } else {
                    currentFavorites.add(meal.idMeal)
                }

                _uiState.value = _uiState.value.copy(favorites = currentFavorites)

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
                val rollbackFavorites = _uiState.value.favorites.toMutableList()
                if (meal.idMeal in rollbackFavorites) {
                    rollbackFavorites.remove(meal.idMeal)
                } else {
                    rollbackFavorites.add(meal.idMeal)
                }
                _uiState.value = _uiState.value.copy(favorites = rollbackFavorites)
            }
        }
    }

    fun toggleFavorite(mealId: String) {
        viewModelScope.launch {
            try {
                val mealFromList = _uiState.value.allMeals.firstOrNull { it.idMeal == mealId }
                val mealFromDetail = (_uiState.value.mealDetailState as? MealDetailUiState.Success)?.meal
                val meal = when {
                    mealFromList != null -> mealFromList
                    mealFromDetail?.idMeal == mealId -> mealFromDetail
                    else -> null
                }

                if (meal != null) {
                    toggleFavorite(meal)
                    return@launch
                }

                val currentFavorites = _uiState.value.favorites.toMutableList()
                if (mealId in currentFavorites) {
                    favouriteService.removeFavourite(mealId)
                    currentFavorites.remove(mealId)
                }
                _uiState.value = _uiState.value.copy(favorites = currentFavorites)
            } catch (_: Exception) {
            }
        }
    }

    fun clearDetail() {
        _uiState.value = _uiState.value.copy(
            selectedMealId = null,
            mealDetailState = null
        )
    }
}
