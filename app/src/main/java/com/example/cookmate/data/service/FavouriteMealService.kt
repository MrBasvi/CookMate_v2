package com.example.cookmate.data.service

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.cookmate.data.db.dao.FavouriteMealDao
import com.example.cookmate.data.db.entity.FavouriteMealEntity
import com.example.cookmate.data.model.Meal
import java.io.File
import java.net.URL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FavouriteMealService @Inject constructor(
    private val favouriteMealDao: FavouriteMealDao,
    @ApplicationContext private val context: Context
) {
    
    fun getAllFavourites(): Flow<List<Meal>> = 
        favouriteMealDao.getAllFavourites().map { entities ->
            entities.map { it.toMeal() }
        }
    
    suspend fun isFavourite(idMeal: String): Boolean =
        favouriteMealDao.getFavourite(idMeal) != null
    
    suspend fun addFavourite(meal: Meal) {
        val localImageUri = saveMealImageLocally(meal.idMeal, meal.strMealThumb)
        val entity = FavouriteMealEntity(
            idMeal = meal.idMeal,
            strMeal = meal.strMeal,
            strCategory = meal.strCategory,
            strArea = meal.strArea,
            strInstructions = meal.strInstructions,
            strMealThumb = localImageUri,
            ingredients = meal.ingredients
        )
        favouriteMealDao.addFavourite(entity)
    }
    
    suspend fun removeFavourite(mealId: String) {
        val current = favouriteMealDao.getFavourite(mealId)
        current?.strMealThumb?.let { maybeDeleteLocalImage(it) }
        favouriteMealDao.removeFavouriteById(mealId)
    }

    private suspend fun saveMealImageLocally(mealId: String, imageUrl: String): String = withContext(Dispatchers.IO) {
        try {
            if (imageUrl.startsWith("file://") || imageUrl.startsWith(context.filesDir.absolutePath)) {
                return@withContext imageUrl
            }

            val imagesDir = File(context.filesDir, "meal_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val imageFile = File(imagesDir, "meal_$mealId.jpg")
            URL(imageUrl).openStream().use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(imageFile).toString()
        } catch (_: Exception) {
            imageUrl
        }
    }

    private fun maybeDeleteLocalImage(imagePath: String) {
        try {
            val file = when {
                imagePath.startsWith("file://") -> {
                    val parsedPath = Uri.parse(imagePath).path ?: return
                    File(parsedPath)
                }
                imagePath.startsWith(context.filesDir.absolutePath) -> File(imagePath)
                else -> return
            }
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
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
