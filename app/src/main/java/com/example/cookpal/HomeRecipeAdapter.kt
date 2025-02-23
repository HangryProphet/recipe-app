package com.example.cookpal

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeRecipeAdapter(
    private var recipeList: MutableList<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<HomeRecipeAdapter.HomeRecipeViewHolder>() {

    class HomeRecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeImage: ImageView = view.findViewById(R.id.recipeImage)
        val recipeName: TextView = view.findViewById(R.id.recipeNameText)
        val cookingTime: TextView = view.findViewById(R.id.cookingTimeText)
        val rating: TextView = view.findViewById(R.id.ratingText)
        val uploaderText: TextView = view.findViewById(R.id.uploaderText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_recipe, parent, false)
        return HomeRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        Log.d("HomeRecipeAdapter", "Binding Recipe: ${recipe.recipeName}")

        holder.recipeName.text = recipe.recipeName
        holder.cookingTime.text = recipe.cookingTime
        holder.rating.text = "⭐ ${recipe.rating}"
        holder.uploaderText.text = "By: ${recipe.uploaderName}"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RecipeActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.recipeId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = recipeList.size

    fun updateList(newList: List<Recipe>) {
        Log.d("HomeRecipeAdapter", "Before updating - Current List Size: ${recipeList.size}")

        // 🔥 Instead of clearing, assign a new list reference
        recipeList.clear()
        recipeList.addAll(newList)

        Log.d("HomeRecipeAdapter", "After updating - New List Size: ${recipeList.size}")
        newList.forEach { Log.d("HomeRecipeAdapter", "Recipe in Adapter: ${it.recipeName}") }

        notifyDataSetChanged()
    }



}
