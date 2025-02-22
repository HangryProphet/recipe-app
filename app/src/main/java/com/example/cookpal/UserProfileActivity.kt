package com.example.cookpal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var profileTitle: TextView
    private lateinit var profileBio: TextView
    private lateinit var followButton: Button
    private lateinit var profileRecipeCount: TextView
    private lateinit var profileFollowersCount: TextView
    private lateinit var profileFollowingCount: TextView
    private lateinit var backButton: ImageView
    private lateinit var profileRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var chipRecipes: Chip
    private lateinit var chipBookmarks: Chip

    private var recipeList = mutableListOf<Recipe>()
    private var viewedUserId: String = ""
    private var isFollowing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get User ID from Intent
        viewedUserId = intent.getStringExtra("VIEWED_USER_ID") ?: return

        // Initialize Views
        profileTitle = findViewById(R.id.profileTitle)
        profileBio = findViewById(R.id.profileBio)
        profileRecipeCount = findViewById(R.id.tvRecipeCount)
        profileFollowersCount = findViewById(R.id.tvFollowersCount)
        profileFollowingCount = findViewById(R.id.tvFollowingCount)
        backButton = findViewById(R.id.backButton)
        profileRecyclerView = findViewById(R.id.profileRecyclerView)
        chipRecipes = findViewById(R.id.chipRecipes)
        chipBookmarks = findViewById(R.id.chipBookmarks)
        followButton = findViewById(R.id.followButton)

        // Set up RecyclerView
        profileRecyclerView.layoutManager = LinearLayoutManager(this)
        recipeAdapter = RecipeAdapter(recipeList, showUploader = false) { selectedRecipe ->
            val intent = Intent(this, RecipeActivity::class.java)
            intent.putExtra("RECIPE_ID", selectedRecipe.recipeId)
            startActivity(intent)
        }
        profileRecyclerView.adapter = recipeAdapter

        // Load User Data & Recipes
        loadUserData()
        loadUserRecipes()

        // Handle Follow Button Click
        followButton.setOnClickListener {
            toggleFollow()
        }

        // Handle Back Button Click
        backButton.setOnClickListener {
            finish()
        }

        // Handle Chip Selection for Recipes & Bookmarks
        chipRecipes.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                loadUserRecipes()
            }
        }

        chipBookmarks.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                loadBookmarkedRecipes()
            }
        }
    }

    private fun loadUserData() {
        db.collection("users").document(viewedUserId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    profileTitle.text = document.getString("username") ?: "User"
                    profileBio.text = document.getString("bio") ?: "No bio yet."

                    val followers = (document.get("followers") as? List<String>)?.size ?: 0
                    val following = (document.get("following") as? List<String>)?.size ?: 0
                    profileFollowersCount.text = "$followers"
                    profileFollowingCount.text = "$following"

                    updateFollowCounts()
                    checkFollowStatus()

                }
            }
    }

    private fun loadUserRecipes() {
        db.collection("recipes")
            .whereEqualTo("userId", viewedUserId)
            .get()
            .addOnSuccessListener { documents ->
                recipeList.clear()
                for (document in documents) {
                    val recipe = Recipe(
                        recipeId = document.id,
                        recipeName = document.getString("recipeName") ?: "Unknown",
                        cookingTime = document.getString("cookingTime") ?: "N/A",
                        rating = document.getDouble("averageRating") ?: 0.0
                    )
                    recipeList.add(recipe)
                }
                profileRecipeCount.text = "${recipeList.size}"
                recipeAdapter.notifyDataSetChanged()
            }
    }

    private fun loadBookmarkedRecipes() {
        db.collection("users").document(viewedUserId).get()
            .addOnSuccessListener { document ->
                val bookmarkedRecipeIds = document.get("bookmarks") as? List<String> ?: emptyList()

                if (bookmarkedRecipeIds.isEmpty()) {
                    recipeList.clear()
                    recipeAdapter.notifyDataSetChanged()
                } else {
                    db.collection("recipes")
                        .whereIn("recipeId", bookmarkedRecipeIds)
                        .get()
                        .addOnSuccessListener { documents ->
                            recipeList.clear()
                            for (document in documents) {
                                val recipe = Recipe(
                                    recipeId = document.id,
                                    recipeName = document.getString("recipeName") ?: "Unknown",
                                    cookingTime = document.getString("cookingTime") ?: "N/A",
                                    rating = document.getDouble("averageRating") ?: 0.0
                                )
                                recipeList.add(recipe)
                            }
                            recipeAdapter.notifyDataSetChanged()
                        }
                }
            }
    }

    private fun checkFollowStatus() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val followingList = document.get("following") as? List<String> ?: emptyList()
                isFollowing = followingList.contains(viewedUserId)
                updateFollowButton()
            }
    }

    private fun toggleFollow() {
        val currentUserId = auth.currentUser?.uid ?: return

        val currentUserRef = db.collection("users").document(currentUserId)
        val viewedUserRef = db.collection("users").document(viewedUserId)

        db.runTransaction { transaction ->
            val currentUserDoc = transaction.get(currentUserRef)
            val viewedUserDoc = transaction.get(viewedUserRef)

            // Retrieve current lists; default to empty mutable lists if they don't exist.
            val currentFollowing = (currentUserDoc.get("following") as? MutableList<String>) ?: mutableListOf()
            val viewedFollowers = (viewedUserDoc.get("followers") as? MutableList<String>) ?: mutableListOf()
            // Retrieve the "following" list from the viewed user to include in the payload (even if unchanged).
            val viewedFollowing = (viewedUserDoc.get("following") as? MutableList<String>) ?: mutableListOf()

            // Update lists based on current follow status.
            if (isFollowing) {
                currentFollowing.remove(viewedUserId)
                viewedFollowers.remove(currentUserId)
            } else {
                currentFollowing.add(viewedUserId)
                viewedFollowers.add(currentUserId)
            }

            // Owner updates their own document partially, so updating just "following" is fine.
            transaction.update(currentUserRef, "following", currentFollowing)
            // For the viewed user, include both "followers" and "following" keys to satisfy security rules.
            transaction.update(viewedUserRef, mapOf(
                "followers" to viewedFollowers,
                "following" to viewedFollowing
            ))
        }.addOnSuccessListener {
            // Toggle the follow status in UI.
            isFollowing = !isFollowing
            updateFollowButton()
            updateFollowCounts() // Refresh follower/following counts.
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to update follow status: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateFollowButton() {
        if (isFollowing) {
            followButton.text = "Unfollow"
            followButton.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
        } else {
            followButton.text = "Follow"
            followButton.setBackgroundColor(ContextCompat.getColor(this, R.color.golden_yellow))
        }
    }

    private fun updateFollowCounts() {
        db.collection("users").document(viewedUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val followers = (document.get("followers") as? List<String>)?.size ?: 0
                    val following = (document.get("following") as? List<String>)?.size ?: 0
                    profileFollowersCount.text = "$followers"
                    profileFollowingCount.text = "$following"
                }
            }

    // Fetch and update FOLLOWING count (logged-in user)
       val currentUserId = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val following = (document.get("following") as? List<String>)?.size ?: 0
                    profileFollowingCount.text = "$following"
                }
            }
    }

}
