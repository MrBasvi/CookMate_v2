package com.example.cookmate.data.repository

import com.example.cookmate.data.api.MealApiService
import com.example.cookmate.data.api.RemoteMeal
import com.example.cookmate.data.model.Ingredient
import com.example.cookmate.data.model.Meal
import javax.inject.Inject

class MealRepository @Inject constructor(
    private val apiService: MealApiService
) {
    
    suspend fun searchMealsByName(name: String): List<Meal> {
        val response = apiService.searchMealsByName(name)
        return response.meals?.map { it.toMeal() } ?: emptyList()
    }
    
    suspend fun getMealDetails(mealId: String): Meal {
        val response = apiService.getMealDetails(mealId)
        val remoteMeal = response.meals?.firstOrNull()
            ?: throw Exception("Рецепт не найден")
        return remoteMeal.toMeal()
    }
    
    private fun RemoteMeal.toMeal(): Meal {
        val ingredients = mutableListOf<Ingredient>()
        
        // Собираем все ингредиенты с мерами
        val ingredientsList = listOf(
            strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5,
            strIngredient6, strIngredient7, strIngredient8, strIngredient9, strIngredient10,
            strIngredient11, strIngredient12, strIngredient13, strIngredient14, strIngredient15,
            strIngredient16, strIngredient17, strIngredient18, strIngredient19, strIngredient20
        )
        
        val measuresList = listOf(
            strMeasure1, strMeasure2, strMeasure3, strMeasure4, strMeasure5,
            strMeasure6, strMeasure7, strMeasure8, strMeasure9, strMeasure10,
            strMeasure11, strMeasure12, strMeasure13, strMeasure14, strMeasure15,
            strMeasure16, strMeasure17, strMeasure18, strMeasure19, strMeasure20
        )
        
        for (i in ingredientsList.indices) {
            val ingredient = ingredientsList[i]
            val measure = measuresList.getOrNull(i)
            
            if (!ingredient.isNullOrBlank()) {
                ingredients.add(
                    Ingredient(
                        name = ingredient.trim(),
                        measure = measure?.trim() ?: ""
                    )
                )
            }
        }
        
        return Meal(
            idMeal = idMeal,
            strMeal = strMeal,
            strCategory = strCategory,
            strArea = strArea,
            strInstructions = strInstructions,
            strMealThumb = strMealThumb,
            ingredients = ingredients
        )
    }
}
