package com.example.cookpal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IngredientsAdapter(private val ingredients: List<String>) : RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder>() {

    inner class IngredientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ingredientText: TextView = view.findViewById(R.id.ingredientText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ingredient, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.ingredientText.text = ingredients[position]
    }

    override fun getItemCount() = ingredients.size
}
