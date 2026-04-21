package com.example.cookmate.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.cookmate.data.db.converter.IngredientConverter
import com.example.cookmate.data.db.dao.FavouriteMealDao
import com.example.cookmate.data.db.entity.FavouriteMealEntity

@Database(
    entities = [FavouriteMealEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(IngredientConverter::class)
abstract class CookMateDatabase : RoomDatabase() {
    abstract fun favouriteMealDao(): FavouriteMealDao
}
