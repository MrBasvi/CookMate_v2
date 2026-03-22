package com.example.cookmate.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cookmate.ui.screens.DetailScreen
import com.example.cookmate.ui.screens.FavoritesScreen
import com.example.cookmate.ui.screens.SearchScreen
import com.example.cookmate.ui.viewmodel.CookMateViewModel

object CookMateRoutes {
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val DETAIL = "detail"
}

@Composable
fun CookMateNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: CookMateViewModel = hiltViewModel()
) {
    val (selectedTab, setSelectedTab) = remember { mutableStateOf(CookMateRoutes.SEARCH) }
    val uiState = viewModel.uiState

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = CookMateRoutes.SEARCH,
            modifier = Modifier.weight(1f)
        ) {
            composable(CookMateRoutes.SEARCH) {
                SearchScreen(
                    viewModel = viewModel,
                    onMealSelected = { mealId ->
                        navController.navigate("${CookMateRoutes.DETAIL}/$mealId")
                    }
                )
            }

            composable(CookMateRoutes.FAVORITES) {
                FavoritesScreen(
                    favoriteMeals = uiState.favoriteMeals,
                    onMealSelected = { mealId ->
                        navController.navigate("${CookMateRoutes.DETAIL}/$mealId")
                    },
                    onToggleFavorite = { mealId ->
                        viewModel.toggleFavorite(mealId)
                    }
                )
            }

            composable("${CookMateRoutes.DETAIL}/{mealId}") { backStackEntry ->
                val mealId = backStackEntry.arguments?.getString("mealId") ?: return@composable

                LaunchedEffect(mealId) {
                    viewModel.getMealDetails(mealId)
                }

                DetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                label = { Text("Поиск") },
                selected = selectedTab == CookMateRoutes.SEARCH,
                onClick = {
                    setSelectedTab(CookMateRoutes.SEARCH)
                    navController.navigate(CookMateRoutes.SEARCH) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Favorite, contentDescription = "Избранное") },
                label = { Text("Избранное") },
                selected = selectedTab == CookMateRoutes.FAVORITES,
                onClick = {
                    setSelectedTab(CookMateRoutes.FAVORITES)
                    navController.navigate(CookMateRoutes.FAVORITES) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
