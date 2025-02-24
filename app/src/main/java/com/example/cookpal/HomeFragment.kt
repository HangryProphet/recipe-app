package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var recipeAdapter: HomeRecipeAdapter
    private lateinit var recommendationsRecyclerView: RecyclerView
    private lateinit var recommendationsAdapter: HomeRecipeAdapter
    private lateinit var cuisineFilterGroup: ChipGroup
    private lateinit var greetingText: TextView
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private val recipeList = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        db = FirebaseFirestore.getInstance()

        recipeRecyclerView = view.findViewById(R.id.topRatedRecyclerView)
        cuisineFilterGroup = view.findViewById(R.id.cuisineFilterGroup)
        greetingText = view.findViewById(R.id.greetingText)
        searchView = view.findViewById(R.id.searchView)
        recommendationsRecyclerView = view.findViewById(R.id.recommendationsRecyclerView)
        recommendationsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        recipeRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recipeAdapter = HomeRecipeAdapter(recipeList) { selectedRecipe ->
            val intent = Intent(requireContext(), RecipeActivity::class.java)
            intent.putExtra("RECIPE_ID", selectedRecipe.recipeId)
            startActivity(intent)
        }
        recipeRecyclerView.adapter = recipeAdapter

        recommendationsAdapter = HomeRecipeAdapter(mutableListOf()) { selectedRecipe ->
            val intent = Intent(requireContext(), RecipeActivity::class.java)
            intent.putExtra("RECIPE_ID", selectedRecipe.recipeId)
            startActivity(intent)
        }
        recommendationsRecyclerView.adapter = recommendationsAdapter

        loadUserGreeting()
        loadAllRecipes()
        loadRandomRecipes(null)

        searchView.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val intent = Intent(requireContext(), SearchActivity::class.java)
                startActivity(intent)
                searchView.clearFocus()
            }
        }

        cuisineFilterGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedChip = view.findViewById<Chip>(checkedId)
            if (selectedChip != null) {
                val cuisine = selectedChip.text.toString()
                if (cuisine == "All") {
                    loadAllRecipes()
                    loadRandomRecipes(null) // ✅ Show any recipes when "All" is selected
                } else {
                    loadTopRatedRecipesByCuisine(cuisine)
                    loadRandomRecipes(cuisine) // ✅ Show only recipes from the selected cuisine
                }
            }
        }

        return view
    }

    private fun loadUserGreeting() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "User"
                greetingText.text = "HELLO, $username"
            }
            .addOnFailureListener {
                Log.e("HomeFragment", "Failed to fetch username")
            }
    }

    private fun loadAllRecipes() {
        db.collection("recipes")
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                recipeList.clear()

                if (documents.isEmpty) {
                    Log.w("HomeFragment", "No recipes found!")
                    recipeRecyclerView.visibility = View.GONE
                } else {
                    recipeRecyclerView.visibility = View.VISIBLE

                    val tempRecipeList = mutableListOf<Recipe>()
                    var loadedCount = 0

                    for (document in documents) {
                        val recipeId = document.id
                        val recipeName = document.getString("recipeName") ?: "Unknown"
                        val cookingTime = document.getString("cookingTime") ?: "N/A"
                        val rating = document.getDouble("averageRating") ?: 0.0
                        val uploaderId = document.getString("userId") ?: ""

                        Log.d(
                            "HomeFragment",
                            "Adding Recipe: $recipeName | Cooking Time: $cookingTime | Rating: $rating"
                        )

                        db.collection("users").document(uploaderId).get()
                            .addOnSuccessListener { userDoc ->
                                val uploaderName = userDoc.getString("username") ?: "Unknown"

                                val recipe = Recipe(
                                    recipeId = recipeId,
                                    recipeName = recipeName,
                                    cookingTime = cookingTime,
                                    rating = rating,
                                    uploaderName = uploaderName
                                )

                                tempRecipeList.add(recipe)
                                loadedCount++

                                if (loadedCount == documents.size()) {
                                    tempRecipeList.sortByDescending { it.rating }
                                    recipeAdapter.updateList(tempRecipeList)

                                    // ✅ Call `loadRandomRecipes(null)` **only if it hasn't been loaded yet**
                                    if (recommendationsAdapter.itemCount == 0) {
                                        loadRandomRecipes(null)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.w("HomeFragment", "Failed to fetch username for $uploaderId")
                                loadedCount++

                                if (loadedCount == documents.size()) {
                                    tempRecipeList.sortByDescending { it.rating }
                                    recipeAdapter.updateList(tempRecipeList)

                                    if (recommendationsAdapter.itemCount == 0) {
                                        loadRandomRecipes(null)
                                    }
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error loading all recipes: ${exception.message}", exception)
            }
    }

    private fun loadTopRatedRecipesByCuisine(cuisine: String) {
        db.collection("recipes")
            .whereEqualTo("cuisineType", cuisine)
            .orderBy("averageRating", Query.Direction.DESCENDING) // 🔥 Ensure sorting by rating
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val tempRecipeList = mutableListOf<Recipe>()

                if (documents.isEmpty) {
                    Log.w("HomeFragment", "No top-rated recipes found for $cuisine")
                    recipeRecyclerView.visibility = View.GONE
                } else {
                    recipeRecyclerView.visibility = View.VISIBLE

                    var loadedCount = 0
                    for (document in documents) {
                        val recipeId = document.id
                        val recipeName = document.getString("recipeName") ?: "Unknown"
                        val cookingTime = document.getString("cookingTime") ?: "N/A"
                        val rating = document.getDouble("averageRating") ?: 0.0
                        val uploaderId = document.getString("userId") ?: ""

                        Log.d(
                            "HomeFragment",
                            "Adding Recipe: $recipeName | Cooking Time: $cookingTime | Rating: $rating"
                        )

                        db.collection("users").document(uploaderId).get()
                            .addOnSuccessListener { userDoc ->
                                val uploaderName = userDoc.getString("username") ?: "Unknown"

                                val recipe = Recipe(
                                    recipeId = recipeId,
                                    recipeName = recipeName,
                                    cookingTime = cookingTime,
                                    rating = rating,
                                    uploaderName = uploaderName
                                )

                                tempRecipeList.add(recipe)
                                loadedCount++

                                if (loadedCount == documents.size()) {
                                    tempRecipeList.sortByDescending { it.rating } // 🔥 FIX: Sort recipes after fetching
                                    recipeAdapter.updateList(tempRecipeList)
                                }
                            }
                            .addOnFailureListener {
                                Log.w("HomeFragment", "Failed to fetch username for $uploaderId")
                                loadedCount++

                                if (loadedCount == documents.size()) {
                                    tempRecipeList.sortByDescending { it.rating }
                                    recipeAdapter.updateList(tempRecipeList)
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "HomeFragment",
                    "Error loading top-rated recipes: ${exception.message}",
                    exception
                )
            }
    }

    private fun loadRandomRecipes(selectedCuisine: String? = null) {
        var query: Query = db.collection("recipes")

        if (!selectedCuisine.isNullOrEmpty() && selectedCuisine != "All") {
            query = query.whereEqualTo("cuisineType", selectedCuisine)
        }

        query.get()
            .addOnSuccessListener { documents ->
                val tempRecipeList = mutableListOf<Recipe>()

                if (documents.isEmpty) {
                    Log.w("HomeFragment", "No random recipes found for $selectedCuisine")
                    recommendationsRecyclerView.visibility = View.GONE
                    recommendationsAdapter.updateList(emptyList()) // ✅ Clear list if no results
                    return@addOnSuccessListener
                } else {
                    recommendationsRecyclerView.visibility = View.VISIBLE
                }

                val uploaderTasks = mutableListOf<Task<DocumentSnapshot>>()

                for (document in documents) {
                    val recipeId = document.id
                    val recipeName = document.getString("recipeName") ?: "Unknown"
                    val cookingTime = document.getString("cookingTime") ?: "N/A"
                    val rating = document.getDouble("averageRating") ?: 0.0
                    val uploaderId = document.getString("userId") ?: ""

                    if (uploaderId.isBlank()) {
                        Log.w("HomeFragment", "Skipping recipe $recipeId due to missing uploaderId")
                        continue
                    }

                    val recipe = Recipe(
                        recipeId = recipeId,
                        recipeName = recipeName,
                        cookingTime = cookingTime,
                        rating = rating,
                        uploaderId = uploaderId
                    )

                    tempRecipeList.add(recipe)

                    val uploaderTask = db.collection("users").document(uploaderId).get()
                    uploaderTasks.add(uploaderTask)
                }

                // ✅ Ensure all uploader names are fetched before updating the adapter
                Tasks.whenAllSuccess<DocumentSnapshot>(uploaderTasks)
                    .addOnSuccessListener { snapshots ->
                        for ((index, snapshot) in snapshots.withIndex()) {
                            tempRecipeList[index].uploaderName =
                                snapshot.getString("username") ?: "Unknown"
                        }

                        tempRecipeList.shuffle() // ✅ Randomize the list before displaying
                        recommendationsAdapter.updateList(tempRecipeList) // ✅ Update UI properly
                    }
                    .addOnFailureListener {
                        Log.e("HomeFragment", "Failed to fetch uploader names for random recipes")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error loading random recipes: ${exception.message}", exception)
            }
    }

}
