package com.example.cookmate.data.db.converter

import androidx.room.TypeConverter
import com.example.cookmate.data.model.Ingredient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IngredientConverter {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromIngredientList(ingredients: List<Ingredient>?): String? {
        return if (ingredients == null) null else gson.toJson(ingredients)
    }
    
    @TypeConverter
    fun toIngredientList(ingredientsString: String?): List<Ingredient>? {
        if (ingredientsString == null) return null
        val type = object : TypeToken<List<Ingredient>>() {}.type
        return gson.fromJson(ingredientsString, type)
    }
}
