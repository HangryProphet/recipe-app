package com.example.cookpal

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class RecipeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recipeNameText: TextView
    private lateinit var cuisineTypeText: TextView
    private lateinit var cookingTimeText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var uploadedByText: TextView
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        db = FirebaseFirestore.getInstance()

        recipeNameText = findViewById(R.id.recipeNameText)
        cuisineTypeText = findViewById(R.id.cuisineTypeText)
        cookingTimeText = findViewById(R.id.cookingTimeText)
        descriptionText = findViewById(R.id.descriptionText)
        uploadedByText = findViewById(R.id.uploadedByText)
        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        stepsContainer = findViewById(R.id.stepsContainer)

        val recipeId = intent.getStringExtra("RECIPE_ID") ?: return

        db.collection("recipes").document(recipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val recipeName = document.getString("recipeName") ?: "Unknown"
                    val cuisineType = document.getString("cuisineType") ?: "Unknown"
                    val cookingTime = document.getString("cookingTime") ?: "N/A"
                    val description = document.getString("shortDescription") ?: "No description available"
                    val userId = document.getString("userId") ?: ""

                    val ingredients = document.get("ingredients") as? List<String> ?: emptyList()
                    val steps = document.get("steps") as? List<String> ?: emptyList()

                    // Set text views
                    recipeNameText.text = recipeName
                    cuisineTypeText.text = "Cuisine: $cuisineType"
                    cookingTimeText.text = "Cooking Time: $cookingTime"
                    descriptionText.text = description

                    // Fetch uploader's username
                    if (userId.isNotEmpty()) {
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val username = userDoc.getString("username") ?: "Unknown User"
                                    uploadedByText.text = "Uploaded by: $username"
                                }
                            }
                    } else {
                        uploadedByText.text = "Uploaded by: Unknown"
                    }

                    // Display ingredients
                    ingredientsContainer.removeAllViews()
                    for (ingredient in ingredients) {
                        val ingredientTextView = TextView(this)
                        ingredientTextView.text = "• $ingredient"
                        ingredientsContainer.addView(ingredientTextView)
                    }

                    // Display steps
                    stepsContainer.removeAllViews()
                    for (step in steps) {
                        val stepTextView = TextView(this)
                        stepTextView.text = "• $step"
                        stepsContainer.addView(stepTextView)
                    }

                } else {
                    Toast.makeText(this, "Recipe not found!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading recipe: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("Firestore", "Error fetching recipe", e)
                finish()
            }
    }
}
