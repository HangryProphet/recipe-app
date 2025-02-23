package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath

class BookmarkFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_bookmark, container, false)

        searchView = view.findViewById(R.id.searchView)
        recipeRecyclerView = view.findViewById(R.id.recipeRecyclerView)

        recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recipeAdapter = RecipeAdapter(recipeList, showUploader = true) { selectedRecipe ->
            val intent = Intent(requireContext(), RecipeActivity::class.java)
            intent.putExtra("RECIPE_ID", selectedRecipe.recipeId)
            startActivity(intent)
        }
        recipeRecyclerView.adapter = recipeAdapter

        loadBookmarkedRecipes()

        // Handle search input
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    Log.d("BookmarkDebug", "Search submitted: $query")
                    filterBookmarks(query)
                } else {
                    Log.d("BookmarkDebug", "Query was empty")
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false // We only search when user presses submit
            }
        })

        return view
    }

    private fun loadBookmarkedRecipes() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val bookmarks = document.get("bookmarks") as? List<String> ?: emptyList()

                if (bookmarks.isEmpty()) {
                    recipeList.clear()
                    recipeAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                db.collection("recipes")
                    .whereIn(FieldPath.documentId(), bookmarks)
                    .get()
                    .addOnSuccessListener { documents ->
                        recipeList.clear()
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
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUploaderNames(recipes: List<Recipe>) {
        val userIds = recipes.map { it.uploaderId }.toSet()

        db.collection("users")
            .whereIn(FieldPath.documentId(), userIds.toList())
            .get()
            .addOnSuccessListener { userDocs ->
                val userMap = mutableMapOf<String, String>()

                for (doc in userDocs) {
                    val userId = doc.id
                    val username = doc.getString("username") ?: "Unknown"
                    userMap[userId] = username
                }

                recipeList.clear()
                for (recipe in recipes) {
                    recipeList.add(recipe.copy(uploaderName = userMap[recipe.uploaderId] ?: "Unknown"))
                }

                recipeAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch uploader names", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterBookmarks(query: String) {
        val lowercaseQuery = query.lowercase()
        val filteredList = recipeList.filter {
            it.recipeName.lowercase().contains(lowercaseQuery)
        }

        recipeAdapter.updateList(filteredList)
    }
}
