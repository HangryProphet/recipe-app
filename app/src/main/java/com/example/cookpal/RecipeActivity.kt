package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RecipeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    // UI elements
    private lateinit var recipeNameText: TextView
    private lateinit var cuisineTypeText: TextView
    private lateinit var shortDescriptionText: TextView
    private lateinit var uploadedByText: TextView
    private lateinit var bookmarkButton: ImageButton
    private lateinit var followButton: Button
    private lateinit var uploaderProfileImage: ImageView
    private lateinit var menuButton: ImageButton
    private lateinit var ratingOverlay: TextView
    private lateinit var reviewCountText: TextView

    // RecyclerViews for content
    private lateinit var ingredientsRecyclerView: RecyclerView
    private lateinit var stepsRecyclerView: RecyclerView
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var recipeChipGroup: ChipGroup

    // Variables
    private var isBookmarked = false
    private var isFollowing = false
    private lateinit var currentUserId: String
    private lateinit var recipeId: String
    private var uploaderId: String = ""

    private val reviewsList = mutableListOf<Review>()
    private lateinit var reviewsAdapter: ReviewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        db = FirebaseFirestore.getInstance()

        // Initialize views
        recipeNameText = findViewById(R.id.recipeNameDetail)
        cuisineTypeText = findViewById(R.id.cuisineTypeDetail)
        shortDescriptionText = findViewById(R.id.shortDescriptionText)
        uploadedByText = findViewById(R.id.uploadedByText)
        bookmarkButton = findViewById(R.id.bookmarkButton)
        menuButton = findViewById(R.id.menuButton)
        followButton = findViewById(R.id.followButton)
        uploaderProfileImage = findViewById(R.id.uploaderProfileImage)
        ingredientsRecyclerView = findViewById(R.id.ingredientsRecyclerView)
        stepsRecyclerView = findViewById(R.id.stepsRecyclerView)
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)
        recipeChipGroup = findViewById(R.id.recipeChipGroup)
        ratingOverlay = findViewById(R.id.ratingsOverlay)
        reviewCountText = findViewById(R.id.reviewCountText)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        recipeId = intent.getStringExtra("RECIPE_ID") ?: ""

        // Set layout managers for RecyclerViews
        ingredientsRecyclerView.layoutManager = LinearLayoutManager(this)
        stepsRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Reviews Adapter
        reviewsAdapter = ReviewsAdapter(reviewsList)
        reviewsRecyclerView.adapter = reviewsAdapter

        bookmarkButton.setBackgroundResource(android.R.color.transparent)

        // Load data
        loadRecipeDetails()
        loadReviews()

        // Button Listeners
        bookmarkButton.setOnClickListener { toggleBookmark() }
        followButton.setOnClickListener { toggleFollowForUploader() }

        db.collection("recipes").document(recipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val recipeName = document.getString("recipeName") ?: "Unknown"
                    val cuisineType = document.getString("cuisineType") ?: "Unknown"
                    val cookingTime = document.getString("cookingTime") ?: "N/A"
                    val shortDesc = document.getString("shortDescription") ?: "No description available"
                    uploaderId = document.getString("userId") ?: ""
                    val averageRating = document.getDouble("averageRating") ?: 0.0
                    val ratingCount = document.getLong("ratingCount") ?: 0

                    val ingredients = document.get("ingredients") as? List<String> ?: emptyList()
                    val steps = document.get("steps") as? List<String> ?: emptyList()
                    val reviews = document.get("reviews") as? List<String> ?: emptyList()

                    // Set UI texts
                    recipeNameText.text = recipeName
                    cuisineTypeText.text = "Cuisine: $cuisineType"
                    shortDescriptionText.text = shortDesc
                    findViewById<TextView>(R.id.cookingTimeOverlay).text = cookingTime
                    ratingOverlay.text = String.format("⭐ %.1f", averageRating)
                    reviewCountText.text = "($ratingCount review${if (ratingCount > 1) "s" else ""})"

                    if (uploaderId == currentUserId) {
                        uploadedByText.visibility = View.GONE
                        uploaderProfileImage.visibility = View.GONE
                    } else {
                        uploadedByText.visibility = View.VISIBLE
                        uploaderProfileImage.visibility = View.VISIBLE

                        db.collection("users").document(uploaderId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val username = userDoc.getString("username") ?: "Unknown User"
                                    uploadedByText.text = username
                                    val openProfile = View.OnClickListener {
                                        val intent = Intent(this, UserProfileActivity::class.java)
                                        intent.putExtra("VIEWED_USER_ID", uploaderId)
                                        startActivity(intent)
                                    }
                                    uploadedByText.setOnClickListener(openProfile)
                                    uploaderProfileImage.setOnClickListener(openProfile)
                                }
                            }
                    }

                    if (uploaderId == currentUserId) {
                        bookmarkButton.visibility = View.GONE
                        followButton.visibility = View.GONE
                        uploaderProfileImage.visibility = View.GONE

                        menuButton.setImageResource(R.drawable.ic_more_horiz)
                        menuButton.setOnClickListener { view ->
                            val popup = PopupMenu(this, view)
                            popup.menuInflater.inflate(R.menu.recipe_owner_menu, popup.menu)
                            popup.setOnMenuItemClickListener { item: MenuItem ->
                                when (item.itemId) {
                                    R.id.menu_edit_recipe -> {
                                        val intent = Intent(this, EditRecipeActivity::class.java).apply {
                                            putExtra("RECIPE_ID", recipeId)
                                        }
                                        startActivity(intent)
                                        true
                                    }
                                    else -> false
                                }
                            }
                            popup.show()
                        }
                    } else {
                        bookmarkButton.visibility = View.VISIBLE
                        followButton.visibility = View.VISIBLE
                        uploaderProfileImage.visibility = View.VISIBLE

                        menuButton.setImageResource(R.drawable.ic_more_horiz)
                        menuButton.setOnClickListener { view ->
                            val popup = PopupMenu(this, view)
                            popup.menuInflater.inflate(R.menu.recipe_menu, popup.menu)
                            popup.setOnMenuItemClickListener { item: MenuItem ->
                                when (item.itemId) {
                                    R.id.menu_review_recipe -> {
                                        val intent = Intent(this, ReviewRecipeActivity::class.java)
                                        intent.putExtra("RECIPE_ID", recipeId)
                                        startActivity(intent)
                                        true
                                    }
                                    R.id.menu_edit_review -> {
                                        loadUserReviewAndOpenEdit()
                                        true
                                    }
                                    R.id.menu_report -> {
                                        reportRecipe()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            popup.show()
                        }
                        checkFollowStatusForUploader()
                    }

                    // Set up RecyclerView adapters
                    ingredientsRecyclerView.adapter = IngredientsAdapter(ingredients)
                    stepsRecyclerView.adapter = StepsAdapter(steps)
                    reviewsRecyclerView.adapter = ReviewsAdapter(reviewsList)

                    // Initially show ingredients only
                    ingredientsRecyclerView.visibility = View.VISIBLE
                    stepsRecyclerView.visibility = View.GONE
                    reviewsRecyclerView.visibility = View.GONE

                    recipeChipGroup.setOnCheckedChangeListener { _, checkedId ->
                        when (checkedId) {
                            R.id.chipIngredients -> {
                                ingredientsRecyclerView.visibility = View.VISIBLE
                                stepsRecyclerView.visibility = View.GONE
                                reviewsRecyclerView.visibility = View.GONE
                            }
                            R.id.chipSteps -> {
                                ingredientsRecyclerView.visibility = View.GONE
                                stepsRecyclerView.visibility = View.VISIBLE
                                reviewsRecyclerView.visibility = View.GONE
                            }
                            R.id.chipReviews -> {
                                ingredientsRecyclerView.visibility = View.GONE
                                stepsRecyclerView.visibility = View.GONE
                                reviewsRecyclerView.visibility = View.VISIBLE
                            }
                        }
                    }

                    // For non-owners, check bookmark status
                    if (uploaderId != currentUserId) {
                        checkBookmarkStatus()
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

        // Set bookmark click listener
        bookmarkButton.setOnClickListener {
            toggleBookmark()
        }

        // Set follow button click listener for uploader
        followButton.setOnClickListener {
            toggleFollowForUploader()
        }
    }
    private fun loadUserReviewAndOpenEdit() {
        val userReviewRef = db.collection("recipes").document(recipeId)
            .collection("reviews").whereEqualTo("userId", currentUserId)

        userReviewRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val userReview = querySnapshot.documents.first() // Get the user's review
                val comment = userReview.getString("comment") ?: ""
                val rating = userReview.getDouble("rating") ?: 0.0

                // Open ReviewRecipeActivity with pre-filled review data
                val intent = Intent(this, ReviewRecipeActivity::class.java)
                intent.putExtra("RECIPE_ID", recipeId)
                intent.putExtra("USER_COMMENT", comment)
                intent.putExtra("USER_RATING", rating.toFloat())
                startActivity(intent)
            }
        }
    }

    private fun loadRecipeDetails() {
        val recipeDocRef = db.collection("recipes").document(recipeId)

        recipeDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val recipeName = document.getString("recipeName") ?: "Unknown"
                val cuisineType = document.getString("cuisineType") ?: "Unknown"
                val cookingTime = document.getString("cookingTime") ?: "N/A"
                val shortDesc = document.getString("shortDescription") ?: "No description available"
                uploaderId = document.getString("userId") ?: ""
                val averageRating = document.getDouble("averageRating") ?: 0.0
                val ratingCount = document.getLong("ratingCount") ?: 0

                recipeNameText.text = recipeName
                cuisineTypeText.text = "Cuisine: $cuisineType"
                shortDescriptionText.text = shortDesc
                findViewById<TextView>(R.id.cookingTimeOverlay).text = cookingTime
                ratingOverlay.text = String.format("%.1f ⭐", averageRating)
                reviewCountText.text = "($ratingCount review${if (ratingCount > 1) "s" else ""})"

                loadUploaderDetails()
            } else {
                Toast.makeText(this, "Recipe not found!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error loading recipe: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun loadUploaderDetails() {
        if (uploaderId == currentUserId) {
            uploadedByText.visibility = View.GONE
            uploaderProfileImage.visibility = View.GONE
        } else {
            uploadedByText.visibility = View.VISIBLE
            uploaderProfileImage.visibility = View.VISIBLE
            db.collection("users").document(uploaderId)
                .get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        val username = userDoc.getString("username") ?: "Unknown User"
                        uploadedByText.text = username
                    }
                }
        }
    }
    private fun loadReviews() {
        val reviewsRef = db.collection("recipes").document(recipeId).collection("reviews")

        reviewsRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val newReviews = mutableListOf<Review>()
                for (doc in querySnapshot.documents) {
                    val review = doc.toObject(Review::class.java)
                    if (review != null) {
                        newReviews.add(review)
                    }
                }

                reviewsList.clear()
                reviewsList.addAll(newReviews)
                reviewsAdapter.notifyDataSetChanged()

                // Update the review count
                reviewCountText.text = "(${newReviews.size} review${if (newReviews.size > 1) "s" else ""})"
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error loading reviews: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Bookmark functions
    private fun checkBookmarkStatus() {
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val bookmarks = document.get("bookmarks") as? List<String> ?: emptyList()
                isBookmarked = bookmarks.contains(recipeId)
                updateBookmarkIcon()
            }
    }

    private fun updateBookmarkIcon() {
        if (isBookmarked) {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_border_full) // filled icon
        } else {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_border_hollow) // outline icon
        }
    }

    private fun toggleBookmark() {
        val userRef = db.collection("users").document(currentUserId)
        if (isBookmarked) {
            userRef.update("bookmarks", FieldValue.arrayRemove(recipeId))
                .addOnSuccessListener {
                    isBookmarked = false
                    updateBookmarkIcon()
                    Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to remove bookmark", Toast.LENGTH_SHORT).show()
                }
        } else {
            userRef.update("bookmarks", FieldValue.arrayUnion(recipeId))
                .addOnSuccessListener {
                    isBookmarked = true
                    updateBookmarkIcon()
                    Toast.makeText(this, "Recipe bookmarked!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to bookmark", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Follow/unfollow functions for uploader
    private fun checkFollowStatusForUploader() {
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val followingList = document.get("following") as? List<String> ?: emptyList()
                isFollowing = followingList.contains(uploaderId)
                updateFollowButton()
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

    private fun toggleFollowForUploader() {
        val currentUserRef = db.collection("users").document(currentUserId)
        val uploaderRef = db.collection("users").document(uploaderId)

        db.runTransaction { transaction ->
            val currentUserDoc = transaction.get(currentUserRef)
            val uploaderDoc = transaction.get(uploaderRef)

            val currentFollowing = (currentUserDoc.get("following") as? MutableList<String>) ?: mutableListOf()
            val uploaderFollowers = (uploaderDoc.get("followers") as? MutableList<String>) ?: mutableListOf()

            if (isFollowing) {
                currentFollowing.remove(uploaderId)
                uploaderFollowers.remove(currentUserId)
            } else {
                currentFollowing.add(uploaderId)
                uploaderFollowers.add(currentUserId)
            }

            transaction.update(currentUserRef, "following", currentFollowing)
            transaction.update(uploaderRef, "followers", uploaderFollowers)
        }.addOnSuccessListener {
            isFollowing = !isFollowing
            updateFollowButton()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to update follow status: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Popup menu option methods
    private fun reportRecipe() {
        Toast.makeText(this, "Reported! Our team will review this recipe.", Toast.LENGTH_LONG).show()
        // Optionally, log/report to Firestore
    }
}
