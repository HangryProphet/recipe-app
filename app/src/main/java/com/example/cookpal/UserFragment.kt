package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class UserFragment : Fragment() {

    private lateinit var settingsButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var profileTitle: TextView
    private lateinit var profileRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var profileChipGroup: ChipGroup
    private lateinit var chipRecipes: Chip
    private lateinit var chipBookmarks: Chip
    private lateinit var profileBio: TextView
    private lateinit var profileRecipeCount: TextView
    private lateinit var profileFollowersCount: TextView
    private lateinit var profileFollowingCount: TextView
    private lateinit var profileFollowersLabel: TextView
    private lateinit var profileFollowingLabel: TextView
    private lateinit var FollowerButtonClick: LinearLayout
    private lateinit var FollowingButtonClick: LinearLayout


    private var recipeList = mutableListOf<Recipe>()
    private var isShowingRecipes = true // Default: show uploaded recipes

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        settingsButton = view.findViewById(R.id.settingsButton)
        profileTitle = view.findViewById(R.id.profileTitle)
        profileRecyclerView = view.findViewById(R.id.profileRecyclerView)
        profileChipGroup = view.findViewById(R.id.profileChipGroup)
        chipRecipes = view.findViewById(R.id.chipRecipes)
        chipBookmarks = view.findViewById(R.id.chipBookmarks)
        profileBio = view.findViewById(R.id.profileBio)
        profileRecipeCount = view.findViewById(R.id.tvRecipeCount)
        profileFollowersCount = view.findViewById(R.id.tvFollowersCount)
        profileFollowingCount = view.findViewById(R.id.tvFollowingCount)
        profileFollowersLabel = view.findViewById(R.id.tvFollowersLabel)
        profileFollowingLabel = view.findViewById(R.id.tvFollowingLabel)
        FollowerButtonClick = view.findViewById(R.id.FollowerButtonClick)
        FollowingButtonClick = view.findViewById(R.id.FollowingButtonClick)

        // Redirect to FollowersActivity when clicking "Followers"
        FollowerButtonClick.setOnClickListener {
            val intent = Intent(requireContext(), FollowersActivity::class.java)
            intent.putExtra("USER_ID", auth.currentUser?.uid) // Pass the user ID
            startActivity(intent)
        }

// Redirect to FollowingActivity when clicking "Following"
        FollowingButtonClick.setOnClickListener {
            val intent = Intent(requireContext(), FollowingActivity::class.java)
            intent.putExtra("USER_ID", auth.currentUser?.uid) // Pass the user ID
            startActivity(intent)
        }


        // Set up RecyclerView
        profileRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recipeAdapter = RecipeAdapter(recipeList, showUploader = false) { selectedRecipe ->
            val intent = Intent(requireContext(), RecipeActivity::class.java)
            intent.putExtra("RECIPE_ID", selectedRecipe.recipeId)
            startActivity(intent)
        }
        profileRecyclerView.adapter = recipeAdapter

        // Load user data
        loadUserData()
        loadUserRecipes()

        profileFollowersLabel.setOnClickListener {
            val intent = Intent(requireContext(), FollowersActivity::class.java)
            intent.putExtra("USER_ID", auth.currentUser!!.uid)
            startActivity(intent)
        }

        profileFollowingCount.setOnClickListener {
            val intent = Intent(requireContext(), FollowingActivity::class.java)
            intent.putExtra("USER_ID", auth.currentUser!!.uid)
            startActivity(intent)
        }


        // Handle Chip Selection
        profileChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipRecipes -> {
                    isShowingRecipes = true
                    loadUserRecipes()
                }
                R.id.chipBookmarks -> {
                    isShowingRecipes = false
                    loadBookmarkedRecipes()
                }
            }
        }

        // Show settings menu when clicking the three dots
        settingsButton.setOnClickListener {
            showSettingsMenu(settingsButton)
        }

        return view
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        loadRecipeCount(userId)

        db.collection("users").document(userId)
            .addSnapshotListener { document, _ ->  // ✅ Real-time listener (updates dynamically)
                if (document != null && document.exists()) {
                    // Fetch username
                    var username = document.getString("username") ?: "User"
                    if (username.length > 15) {
                        username = "${username.take(12)}..."
                        profileTitle.textSize = 22f
                    } else {
                        profileTitle.textSize = 24f
                    }
                    profileTitle.text = username

                    // Fetch bio
                    profileBio.text = document.getString("bio") ?: "No bio yet."

                    // Fetch follower & following count dynamically
                    val followers = (document.get("followers") as? List<String>)?.size ?: 0
                    val following = (document.get("following") as? List<String>)?.size ?: 0
                    profileFollowersCount.text = "$followers"
                    profileFollowingCount.text = "$following"

                    // If followers & following fields don't exist, initialize them
                    val updates = mutableMapOf<String, Any>()
                    if (!document.contains("followers")) updates["followers"] = emptyList<String>()
                    if (!document.contains("following")) updates["following"] = emptyList<String>()

                    if (updates.isNotEmpty()) {
                        db.collection("users").document(userId).update(updates)
                    }
                }
            }
    }

    private fun loadRecipeCount(userId: String) {
        db.collection("recipes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                profileRecipeCount.text = "$count"
            }
    }

    private fun loadFollowCounts(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val followers = (document.get("followers") as? List<String>)?.size ?: 0
                val following = (document.get("following") as? List<String>)?.size ?: 0

                profileFollowersCount.text = "$followers"
                profileFollowingCount.text = "$following"
            }
    }


    private fun loadUserRecipes() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("recipes")
            .whereEqualTo("userId", userId) // Fetch only user's uploaded recipes
            .get()
            .addOnSuccessListener { documents ->
                recipeList.clear()
                for (document in documents) {
                    val recipe = Recipe(
                        recipeId = document.id,
                        recipeName = document.getString("recipeName") ?: "Unknown",
                        cookingTime = document.getString("cookingTime") ?: "N/A",
                        rating = document.getDouble("averageRating") ?: 0.0
                    )
                    recipeList.add(recipe)
                }
                recipeAdapter.notifyDataSetChanged()
            }
    }

    private fun loadBookmarkedRecipes() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val bookmarkedRecipeIds = document.get("bookmarks") as? List<String> ?: emptyList()

                if (bookmarkedRecipeIds.isEmpty()) {
                    recipeList.clear()
                    recipeAdapter.notifyDataSetChanged() // ✅ Notify adapter even when empty!
                    return@addOnSuccessListener
                }

                db.collection("recipes")
                    .whereIn(FieldPath.documentId(), bookmarkedRecipeIds)
                    .get()
                    .addOnSuccessListener { documents ->
                        recipeList.clear()
                        val uploaderTasks = mutableListOf<Task<DocumentSnapshot>>()
                        val tempRecipes = mutableListOf<Recipe>() // Temporary list

                        for (doc in documents) {
                            val recipe = Recipe(
                                recipeId = doc.id,
                                recipeName = doc.getString("recipeName") ?: "Unknown",
                                cookingTime = doc.getString("cookingTime") ?: "N/A",
                                rating = doc.getDouble("averageRating") ?: 0.0,
                                uploaderId = doc.getString("userId") ?: ""
                            )
                            tempRecipes.add(recipe)

                            // 🔥 Fetch uploader name asynchronously
                            val uploaderTask = db.collection("users").document(recipe.uploaderId).get()
                            uploaderTasks.add(uploaderTask)
                        }

                        // 🔥 Ensure uploader names are set **before updating the adapter**
                        Tasks.whenAllSuccess<DocumentSnapshot>(uploaderTasks)
                            .addOnSuccessListener { snapshots ->
                                for ((index, snapshot) in snapshots.withIndex()) {
                                    tempRecipes[index].uploaderName = snapshot.getString("username") ?: "Unknown"
                                }

                                // 🔄 Now update the list after uploader names are fully fetched
                                recipeList.clear()
                                recipeList.addAll(tempRecipes)
                                recipeAdapter.notifyDataSetChanged()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error loading bookmarks.", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun showSettingsMenu(anchor: ImageView) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.user_profile_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_edit_profile -> {
                    showEditBioDialog()
                    true
                }
                R.id.menu_settings -> {
                    // TODO: Navigate to Settings Activity
                    true
                }
                R.id.menu_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showEditBioDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter new bio (max 255 characters)"
            setText(profileBio.text.toString())
            maxLines = 5
            isSingleLine = false
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Bio")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newBio = editText.text.toString().trim()

                if (newBio.length > 255) {
                    Toast.makeText(requireContext(), "Bio cannot exceed 255 characters.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                db.collection("users").document(auth.currentUser!!.uid)
                    .update("bio", newBio)
                    .addOnSuccessListener {
                        profileBio.text = newBio
                        Toast.makeText(requireContext(), "Bio updated!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update bio.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
