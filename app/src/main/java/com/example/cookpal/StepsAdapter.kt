package com.example.cookpal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StepsAdapter(private val steps: List<String>) : RecyclerView.Adapter<StepsAdapter.StepViewHolder>() {

    inner class StepViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stepNumberText: TextView = view.findViewById(R.id.stepNumberText)
        val stepDescriptionText: TextView = view.findViewById(R.id.stepDescriptionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.stepNumberText.text = "Step ${position + 1}:"
        holder.stepDescriptionText.text = steps[position]
    }

    override fun getItemCount() = steps.size
}
