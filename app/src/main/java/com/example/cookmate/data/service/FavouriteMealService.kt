package com.example.cookmate.data.service

import com.example.cookmate.data.db.dao.FavouriteMealDao
import com.example.cookmate.data.db.entity.FavouriteMealEntity
import com.example.cookmate.data.model.Meal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavouriteMealService @Inject constructor(
    private val favouriteMealDao: FavouriteMealDao
) {
    fun getAllFavourites(): Flow<List<Meal>> =
        favouriteMealDao.getAllFavourites().map { entities ->
            entities.map { it.toMeal() }
        }

    suspend fun getFavourite(mealId: String): Meal? =
        favouriteMealDao.getFavourite(mealId)?.toMeal()

    suspend fun addFavourite(meal: Meal) {
        val entity = FavouriteMealEntity(
            idMeal = meal.idMeal,
            strMeal = meal.strMeal,
            strCategory = meal.strCategory,
            strArea = meal.strArea,
            strInstructions = meal.strInstructions,
            strMealThumb = meal.strMealThumb,
            ingredients = meal.ingredients
        )
        favouriteMealDao.addFavourite(entity)
    }

    suspend fun removeFavourite(mealId: String) {
        favouriteMealDao.removeFavouriteById(mealId)
    }

    private fun FavouriteMealEntity.toMeal() = Meal(
        idMeal = idMeal,
        strMeal = strMeal,
        strCategory = strCategory,
        strArea = strArea,
        strInstructions = strInstructions,
        strMealThumb = strMealThumb,
        ingredients = ingredients
    )
}
