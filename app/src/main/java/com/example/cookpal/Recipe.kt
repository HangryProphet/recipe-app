package com.example.cookpal

data class Recipe(
    val recipeId: String = "",
    val recipeName: String = "",
    val cookingTime: String = "",
    val rating: Double = 0.0,
    val uploaderId: String = "",
    var uploaderName: String = ""
)

