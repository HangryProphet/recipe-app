package com.example.cookpal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UploadFragment : Fragment() {

    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var addIngredientButton: Button
    private lateinit var addStepButton: Button
    private lateinit var saveRecipeButton: Button
    private lateinit var cancelText: TextView
    private lateinit var cuisineDropdown: Spinner
    private lateinit var cookingTimeHours: Spinner
    private lateinit var cookingTimeMinutes: Spinner
    private lateinit var recipeNameInput: EditText
    private lateinit var descriptionInput: EditText
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_upload, container, false)

        // Find views
        ingredientsContainer = view.findViewById(R.id.ingredientsContainer)
        stepsContainer = view.findViewById(R.id.stepsContainer)
        addIngredientButton = view.findViewById(R.id.addIngredientButton)
        addStepButton = view.findViewById(R.id.addStepButton)
        saveRecipeButton = view.findViewById(R.id.saveRecipeButton)
        cancelText = view.findViewById(R.id.cancelText)
        cuisineDropdown = view.findViewById(R.id.cuisineDropdown)
        cookingTimeHours = view.findViewById(R.id.cookingTimeHours)
        cookingTimeMinutes = view.findViewById(R.id.cookingTimeMinutes)
        recipeNameInput = view.findViewById(R.id.recipeNameInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)

        // Set up cuisine dropdown
        val cuisines = arrayOf("Asian", "Chinese", "Filipino", "Indian", "Mixed")
        val cuisineAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cuisines)
        cuisineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cuisineDropdown.adapter = cuisineAdapter

        // Set up hours dropdown (0 to 10 hours)
        val hours = Array(11) { "$it hr" }
        val hoursAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, hours)
        hoursAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cookingTimeHours.adapter = hoursAdapter

        // Set up minutes dropdown (0 to 59 minutes)
        val minutes = Array(60) { "$it min" }
        val minutesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, minutes)
        minutesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cookingTimeMinutes.adapter = minutesAdapter

        // Add ingredient field dynamically
        addIngredientButton.setOnClickListener {
            addInputField(ingredientsContainer)
        }

        // Add step field dynamically
        addStepButton.setOnClickListener {
            addInputField(stepsContainer)
        }

        // Show confirmation dialog when cancel button is clicked
        cancelText.setOnClickListener {
            showCancelDialog()
        }

        // Save recipe to Firestore
        saveRecipeButton.setOnClickListener {
            saveRecipeToFirestore()
        }

        return view
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Upload")
            .setMessage("Are you sure you want to cancel uploading? Your progress will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, HomeFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit()

                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                bottomNav.selectedItemId = R.id.nav_home
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun saveRecipeToFirestore() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "You must be logged in to upload a recipe.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid
        val recipeName = recipeNameInput.text.toString().trim()
        val cuisineType = cuisineDropdown.selectedItem.toString()
        val description = descriptionInput.text.toString().trim()

        val formattedRecipeName = recipeName.lowercase()

        // Format Cooking Time
        val selectedHours = cookingTimeHours.selectedItem.toString().replace(" hr", "").toInt()
        val selectedMinutes = cookingTimeMinutes.selectedItem.toString().replace(" min", "").toInt()
        val formattedCookingTime = when {
            selectedHours > 0 && selectedMinutes > 0 -> "$selectedHours hr $selectedMinutes min"
            selectedHours > 0 -> "$selectedHours hr"
            selectedMinutes > 0 -> "$selectedMinutes min"
            else -> "N/A" // If both are 0
        }

        val ingredients = ingredientsContainer.children.mapNotNull { view ->
            (view as? LinearLayout)?.getChildAt(0)?.let { (it as EditText).text.toString().trim() }
        }.filter { it.isNotEmpty() }.toList()

        val steps = stepsContainer.children.mapNotNull { view ->
            (view as? LinearLayout)?.getChildAt(0)?.let { (it as EditText).text.toString().trim() }
        }.filter { it.isNotEmpty() }.toList()

        if (recipeName.isEmpty() || description.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(requireContext(), "All fields must be filled out.", Toast.LENGTH_SHORT).show()
            return
        }

        val recipeData = hashMapOf(
            "userId" to userId,
            "recipeName" to formattedRecipeName,
            "cuisineType" to cuisineType,
            "cookingTime" to formattedCookingTime,
            "shortDescription" to description,
            "ingredients" to ingredients,
            "steps" to steps,
            "timestamp" to System.currentTimeMillis(),
            "ratingCount" to 0,
            "averageRating" to 0.0
        )

        db.collection("recipes")
            .add(recipeData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Recipe uploaded successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, HomeFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit()

                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                bottomNav.selectedItemId = R.id.nav_home
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to upload recipe: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addInputField(container: LinearLayout) {
        val inputLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val editText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = if (container.id == R.id.ingredientsContainer) "Ingredient" else "Step"
            textSize = 16f
            setPadding(12, 12, 12, 12)
            background = resources.getDrawable(R.drawable.rounded_edittext_bg, null)
        }

        val deleteButton = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40).apply { setMargins(8, 0, 0, 0) }
            setImageResource(R.drawable.ic_delete)
            setOnClickListener { container.removeView(inputLayout) }
        }

        inputLayout.addView(editText)
        inputLayout.addView(deleteButton)
        container.addView(inputLayout)
    }
}
