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
    private var recipeList: MutableList<Recipe>,
    private val showUploader: Boolean, // Determines if uploader name is shown (Bookmarks only)
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
        holder.rating.text = "${recipe.rating}"

        // ✅ Show uploader name only if `showUploader` is true
        if (showUploader) {
            holder.uploaderText.text = "By: ${recipe.uploaderName}"
            holder.uploaderText.setTextColor(Color.BLACK)
            holder.uploaderText.visibility = View.VISIBLE

            // 🔥 Redirect to uploader's profile
            holder.uploaderText.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, UserProfileActivity::class.java)
                intent.putExtra("VIEWED_USER_ID", recipe.uploaderId)
                context.startActivity(intent)
            }
        } else {
            holder.uploaderText.visibility = View.GONE // ✅ Hide uploader name for personal recipes
        }

        // ✅ Handle recipe item click (Open RecipeActivity)
        holder.itemView.setOnClickListener { onRecipeClick(recipe) }
    }

    override fun getItemCount() = recipeList.size

    /**
     * 🔥 Function to update the list dynamically (for search filtering, bookmarks, etc.)
     */
    fun updateList(newList: List<Recipe>) {
        recipeList.clear()
        recipeList.addAll(newList)
        notifyDataSetChanged()
    }
}
