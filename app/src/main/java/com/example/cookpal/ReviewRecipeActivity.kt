package com.example.cookpal

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ReviewRecipeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var ratingBar: RatingBar
    private lateinit var reviewEditText: EditText
    private lateinit var submitReviewButton: Button
    private lateinit var recipeId: String
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_recipe)

        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        recipeId = intent.getStringExtra("RECIPE_ID") ?: ""

        ratingBar = findViewById(R.id.ratingBar)
        reviewEditText = findViewById(R.id.reviewEditText)
        submitReviewButton = findViewById(R.id.submitReviewButton)

        // 🔥 Load existing review if passed from RecipeActivity
        val receivedComment = intent.getStringExtra("USER_COMMENT") ?: ""
        val receivedRating = intent.getFloatExtra("USER_RATING", 0f)

        if (receivedRating > 0) {
            ratingBar.rating = receivedRating
        }
        reviewEditText.setText(receivedComment)

        submitReviewButton.setOnClickListener {
            val rating = ratingBar.rating
            val comment = reviewEditText.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
            } else {
                submitOrUpdateReview(rating, comment)
            }
        }
    }

    private fun submitOrUpdateReview(rating: Float, comment: String) {
        val recipeRef = db.collection("recipes").document(recipeId)
        val userReviewRef = recipeRef.collection("reviews").whereEqualTo("userId", currentUserId)

        userReviewRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                // 🔥 If a review exists, update it
                val reviewId = querySnapshot.documents.first().id
                recipeRef.collection("reviews").document(reviewId)
                    .update("rating", rating, "comment", comment, "timestamp", System.currentTimeMillis())
                    .addOnSuccessListener {
                        updateRecipeAverageRating(recipeRef)
                        Toast.makeText(this, "Review updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update review.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // 🔥 If no review exists, add a new one
                val newReview = mapOf(
                    "userId" to currentUserId,
                    "rating" to rating,
                    "comment" to comment,
                    "timestamp" to System.currentTimeMillis()
                )

                recipeRef.collection("reviews").add(newReview)
                    .addOnSuccessListener {
                        updateRecipeAverageRating(recipeRef)
                        Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to submit review.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateRecipeAverageRating(recipeRef: DocumentReference) {
        recipeRef.collection("reviews").get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val allRatings = querySnapshot.documents.mapNotNull { it.getDouble("rating") }
                    val newAvgRating = allRatings.average()

                    recipeRef.update(
                        "averageRating", newAvgRating,
                        "ratingCount", allRatings.size.toLong()
                    )
                }
            }
    }
}
