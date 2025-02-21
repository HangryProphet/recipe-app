package com.example.cookpal

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecipeAdapter(
    private val recipeList: List<Recipe>,
    private val showUploader: Boolean, // Determines whether to show uploader name
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeImage: ImageView = view.findViewById(R.id.recipeImage)
        val recipeName: TextView = view.findViewById(R.id.recipeNameText)
        val cookingTime: TextView = view.findViewById(R.id.cookingTimeText)
        val rating: TextView = view.findViewById(R.id.ratingText)
        val uploaderText: TextView = view.findViewById(R.id.uploaderText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.recipeName.text = recipe.recipeName
        holder.cookingTime.text = recipe.cookingTime
        holder.rating.text = recipe.rating.toString()

        // Ensure uploader name is displayed properly
        if (showUploader) {
            holder.uploaderText.text = "Uploaded by: ${recipe.uploaderName}"
            holder.uploaderText.setTextColor(Color.BLACK)
            holder.uploaderText.visibility = View.VISIBLE

            // Redirect to user's profile
            holder.uploaderText.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, UserProfileActivity::class.java)
                intent.putExtra("VIEWED_USER_ID", recipe.uploaderId)
                context.startActivity(intent)
            }
        } else {
            holder.uploaderText.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onRecipeClick(recipe) }
    }

    override fun getItemCount() = recipeList.size
}
