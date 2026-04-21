package com.example.cookmate.di

import android.content.Context
import androidx.room.Room
import com.example.cookmate.data.db.CookMateDatabase
import com.example.cookmate.data.db.dao.FavouriteMealDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): CookMateDatabase = Room.databaseBuilder(
        context,
        CookMateDatabase::class.java,
        "cook_mate_database"
    ).build()
    
    @Singleton
    @Provides
    fun provideFavouriteMealDao(database: CookMateDatabase): FavouriteMealDao =
        database.favouriteMealDao()
}
