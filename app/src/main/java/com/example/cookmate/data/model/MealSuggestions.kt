package com.example.cookmate.data.model

object MealSuggestions {
    val russianToEnglish = mapOf(
        "паста" to "Pasta",
        "макароны" to "Pasta",
        "курица" to "Chicken",
        "рыба" to "Fish",
        "салат" to "Salad",
        "суп" to "Soup",
        "говядина" to "Beef",
        "свинина" to "Pork",
        "морепродукты" to "Seafood",
        "десерт" to "Dessert",
        "пицца" to "Pizza",
        "бургер" to "Burger",
        "тако" to "Taco",
        "мясо" to "Meat",
        "овощи" to "Vegetable",
        "сладкое" to "Cake",
        "торт" to "Cake",
        "печенье" to "Cookies",
        "хлеб" to "Bread",
        "каша" to "Rice"
    )

    fun translateToEnglish(russianQuery: String): String {
        val lowercase = russianQuery.lowercase()
        return russianToEnglish[lowercase] ?: russianQuery
    }
}
