package com.example.cookpal

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditRecipeActivity : AppCompatActivity() {

    private lateinit var recipeNameInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var cuisineDropdown: Spinner
    private lateinit var cookingTimeHours: Spinner
    private lateinit var cookingTimeMinutes: Spinner
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var saveRecipeButton: Button
    private lateinit var cancelText: TextView
    private lateinit var addIngredientButton: Button
    private lateinit var addStepButton: Button

    private val db = FirebaseFirestore.getInstance()
    private var recipeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_recipe)

        // Initialize Views
        recipeNameInput = findViewById(R.id.recipeNameInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        cuisineDropdown = findViewById(R.id.cuisineDropdown)
        cookingTimeHours = findViewById(R.id.cookingTimeHours)
        cookingTimeMinutes = findViewById(R.id.cookingTimeMinutes)
        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        stepsContainer = findViewById(R.id.stepsContainer)
        saveRecipeButton = findViewById(R.id.saveRecipeButton)
        cancelText = findViewById(R.id.cancelText)
        addIngredientButton = findViewById(R.id.addIngredientButton)
        addStepButton = findViewById(R.id.addStepButton)

        setupDropdowns()

        // Get Recipe ID from intent
        recipeId = intent.getStringExtra("RECIPE_ID")
        recipeId?.let { loadExistingRecipe(it) }

        addIngredientButton.setOnClickListener { addInputField(ingredientsContainer) }
        addStepButton.setOnClickListener { addInputField(stepsContainer) }

        cancelText.setOnClickListener { finish() }

        saveRecipeButton.setOnClickListener { updateRecipeInFirestore() }
    }

    private fun setupDropdowns() {
        val cuisines = arrayOf("Asian", "Chinese", "Filipino", "Indian", "Mixed")
        val cuisineAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cuisines)
        cuisineDropdown.adapter = cuisineAdapter

        val hours = Array(11) { "$it hr" }
        val hoursAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hours)
        cookingTimeHours.adapter = hoursAdapter

        val minutes = Array(60) { "$it min" }
        val minutesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, minutes)
        cookingTimeMinutes.adapter = minutesAdapter
    }

    private fun loadExistingRecipe(recipeId: String) {
        db.collection("recipes").document(recipeId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                recipeNameInput.setText(document.getString("recipeName"))
                descriptionInput.setText(document.getString("shortDescription"))

                val cuisineType = document.getString("cuisineType") ?: ""
                val cuisinePosition = (cuisineDropdown.adapter as ArrayAdapter<String>).getPosition(cuisineType)
                cuisineDropdown.setSelection(cuisinePosition)

                setCookingTimeDropdown(document.getString("cookingTime") ?: "")

                ingredientsContainer.removeAllViews()
                (document.get("ingredients") as? List<String>)?.forEach { addInputField(ingredientsContainer, it) }

                stepsContainer.removeAllViews()
                (document.get("steps") as? List<String>)?.forEach { addInputField(stepsContainer, it) }
            }
        }
    }

    private fun setCookingTimeDropdown(cookingTime: String) {
        var hours = 0
        var minutes = 0

        when {
            cookingTime.contains("hr") && cookingTime.contains("min") -> {
                val parts = cookingTime.split(" ")
                hours = parts[0].replace(" hr", "").toIntOrNull() ?: 0
                minutes = parts[2].replace(" min", "").toIntOrNull() ?: 0
            }
            cookingTime.contains("hr") -> {
                hours = cookingTime.replace(" hr", "").toIntOrNull() ?: 0
            }
            cookingTime.contains("min") -> {
                minutes = cookingTime.replace(" min", "").toIntOrNull() ?: 0
            }
            else -> {
                hours = 0
                minutes = 0
            }
        }

        cookingTimeHours.setSelection(hours)
        cookingTimeMinutes.setSelection(minutes)
    }


    private fun updateRecipeInFirestore() {
        if (recipeId.isNullOrEmpty()) return

        val formattedCookingTime = when {
            cookingTimeHours.selectedItem.toString() != "0 hr" && cookingTimeMinutes.selectedItem.toString() != "0 min" ->
                "${cookingTimeHours.selectedItem} ${cookingTimeMinutes.selectedItem}"
            cookingTimeHours.selectedItem.toString() != "0 hr" -> cookingTimeHours.selectedItem.toString()
            cookingTimeMinutes.selectedItem.toString() != "0 min" -> cookingTimeMinutes.selectedItem.toString()
            else -> "N/A"
        }

        val recipeData = hashMapOf(
            "recipeName" to recipeNameInput.text.toString().trim(),
            "cuisineType" to cuisineDropdown.selectedItem.toString(),
            "cookingTime" to formattedCookingTime,
            "shortDescription" to descriptionInput.text.toString().trim(),
            "ingredients" to getDynamicInputValues(ingredientsContainer),
            "steps" to getDynamicInputValues(stepsContainer)
        )

        db.collection("recipes").document(recipeId!!)
            .update(recipeData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Recipe updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update recipe.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addInputField(container: LinearLayout, existingText: String = "") {
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val editText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setText(existingText)
            hint = if (container.id == R.id.ingredientsContainer) "Ingredient" else "Step"
            textSize = 16f
            setPadding(12, 12, 12, 12)
            background = resources.getDrawable(R.drawable.rounded_edittext_bg, null)
        }

        val deleteButton = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40).apply { setMargins(8, 0, 0, 0) }
            setImageResource(R.drawable.ic_delete)
            setOnClickListener { container.removeView(inputLayout) }
        }

        inputLayout.addView(editText)
        inputLayout.addView(deleteButton)
        container.addView(inputLayout)
    }

    private fun getDynamicInputValues(container: LinearLayout): List<String> {
        return (0 until container.childCount).mapNotNull { index ->
            val layout = container.getChildAt(index) as? LinearLayout
            val editText = layout?.getChildAt(0) as? EditText
            editText?.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
