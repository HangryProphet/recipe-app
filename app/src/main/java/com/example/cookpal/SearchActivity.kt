package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class SearchActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var backButton: androidx.appcompat.widget.AppCompatImageView
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchView = findViewById(R.id.searchView)
        backButton = findViewById(R.id.backButton)
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView)

        recipeRecyclerView.layoutManager = LinearLayoutManager(this)
        recipeAdapter = RecipeAdapter(recipeList, showUploader = true) { selectedRecipe ->
            val intent = Intent(this, RecipeActivity::class.java)
            intent.putExtra("RECIPE_ID", selectedRecipe.recipeId)
            startActivity(intent)
        }
        recipeRecyclerView.adapter = recipeAdapter

        backButton.setOnClickListener {
            finish()
            overridePendingTransition(0, R.anim.slide_out_down)
        }

        // Handle search input
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    Log.d("SearchDebug", "Search submitted: $query")
                    fetchRecipes(query)
                } else {
                    Log.d("SearchDebug", "Query was empty")
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false // We only search when user presses submit
            }
        })
    }

    private fun fetchRecipes(query: String) {
        val lowercaseQuery = query.lowercase()

        db.collection("recipes")
            .whereGreaterThanOrEqualTo("recipeName", lowercaseQuery)
            .whereLessThanOrEqualTo("recipeName", lowercaseQuery + "\uf8ff") // Case-insensitive
            .get()
            .addOnSuccessListener { documents ->
                recipeList.clear()
                if (documents.isEmpty) {
                    Log.d("SearchDebug", "No recipes found for $query")
                }

                val recipesToFetchUploader = mutableListOf<Recipe>()

                for (document in documents) {
                    val recipe = Recipe(
                        recipeId = document.id,
                        recipeName = document.getString("recipeName") ?: "Unknown",
                        cookingTime = document.getString("cookingTime") ?: "N/A",
                        rating = document.getDouble("averageRating") ?: 0.0,
                        uploaderId = document.getString("userId") ?: ""
                    )
                    recipesToFetchUploader.add(recipe)
                }

                if (recipesToFetchUploader.isNotEmpty()) {
                    fetchUploaderNames(recipesToFetchUploader)
                } else {
                    recipeAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load recipes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUploaderNames(recipes: List<Recipe>) {
        val userIds = recipes.map { it.uploaderId }.toSet() // Get unique uploader IDs

        db.collection("users")
            .whereIn(FieldPath.documentId(), userIds.toList()) // ✅ Correct: Query by document ID
            .get()
            .addOnSuccessListener { userDocs ->
                val userMap = mutableMapOf<String, String>() // userId -> username

                for (doc in userDocs) {
                    val userId = doc.id
                    val username = doc.getString("username") ?: "Unknown"
                    userMap[userId] = username
                }
                Log.d("SearchDebug", "User Map: $userMap") // Check if userMap has correct usernames

                // Update recipe list with uploader names
                recipeList.clear()
                for (recipe in recipes) {
                    recipeList.add(
                        recipe.copy(
                            uploaderName = userMap[recipe.uploaderId] ?: "Unknown"
                        )
                    )
                }

                recipeAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch uploader names", Toast.LENGTH_SHORT).show()
            }
    }
}
