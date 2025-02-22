package com.example.cookpal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ReviewsAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val reviewerNameText: TextView = view.findViewById(R.id.reviewerNameText)
        val reviewText: TextView = view.findViewById(R.id.reviewText)
        val ratingText: TextView = view.findViewById(R.id.ratingText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.reviewText.text = review.comment
        holder.ratingText.text = "${review.rating} ★"

        // Fetch the reviewer's username (if available)
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(review.userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val username = doc.getString("username") ?: "Unknown User"
                    holder.reviewerNameText.text = username
                }
            }
            .addOnFailureListener {
                holder.reviewerNameText.text = "Unknown User"
            }
    }

    override fun getItemCount() = reviews.size
}
