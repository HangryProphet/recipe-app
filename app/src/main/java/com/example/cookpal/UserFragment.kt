package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
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

        // Redirect to FollowersActivity when clicking "Followers"
        profileFollowersCount.setOnClickListener {
            val intent = Intent(requireContext(), FollowersActivity::class.java)
            intent.putExtra("USER_ID", auth.currentUser?.uid) // Pass the user ID
            startActivity(intent)
        }

// Redirect to FollowingActivity when clicking "Following"
        profileFollowingCount.setOnClickListener {
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

        profileFollowersCount.setOnClickListener {
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

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Fetch username
                    var username = document.getString("username") ?: "User"
                    if (username.length > 15) {
                        username = "${username.take(12)}..."
                        profileTitle.textSize = 22f
                    } else {
                        profileTitle.textSize = 24f
                    }
                    profileTitle.text = username

                    // Fetch bio (initialize if missing)
                    val bio = document.getString("bio") ?: "No bio yet."
                    profileBio.text = bio
                    if (!document.contains("bio")) {
                        db.collection("users").document(userId).update("bio", "No bio yet.")
                    }

                    // Fetch follower & following count (initialize if missing)
                    val followers = (document.get("followers") as? List<String>)?.size ?: 0
                    val following = (document.get("following") as? List<String>)?.size ?: 0
                    profileFollowersCount.text = "$followers"
                    profileFollowingCount.text = "$following"

                    if (!document.contains("followers")) {
                        db.collection("users").document(userId).update("followers", emptyList<String>())
                    }
                    if (!document.contains("following")) {
                        db.collection("users").document(userId).update("following", emptyList<String>())
                    }
                }
            }
            .addOnFailureListener {
                profileTitle.text = "Profile"
                profileBio.text = "No bio available."
                profileFollowersCount.text = "0"
                profileFollowingCount.text = "0"
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
                    recipeAdapter.notifyDataSetChanged()
                } else {
                    db.collection("recipes")
                        .whereIn("recipeId", bookmarkedRecipeIds)
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
            }
    }

    private fun showSettingsMenu(anchor: ImageView) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.user_profile_menu, popup.menu)

        val viewedUserId = arguments?.getString("VIEWED_USER_ID")
        val currentUserId = auth.currentUser?.uid ?: return

        // Check if this is another user's profile
        if (viewedUserId != null && viewedUserId != currentUserId) {
            // Dynamically add Follow/Unfollow option
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    val followingList = document.get("following") as? List<String> ?: emptyList()
                    val isFollowing = followingList.contains(viewedUserId)

                    val followMenuItem = if (isFollowing) {
                        popup.menu.add("Unfollow")
                    } else {
                        popup.menu.add("Follow")
                    }

                    followMenuItem.setOnMenuItemClickListener {
                        toggleFollow(currentUserId, viewedUserId)
                        true
                    }

                    popup.show()
                }
        } else {
            popup.show()
        }

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
    }

    private fun toggleFollow(currentUserId: String, viewedUserId: String) {
        db.runTransaction { transaction ->
            val currentUserRef = db.collection("users").document(currentUserId)
            val viewedUserRef = db.collection("users").document(viewedUserId)

            val currentUserDoc = transaction.get(currentUserRef)
            val viewedUserDoc = transaction.get(viewedUserRef)

            val followingList = (currentUserDoc.get("following") as? MutableList<String>) ?: mutableListOf()
            val followersList = (viewedUserDoc.get("followers") as? MutableList<String>) ?: mutableListOf()

            if (followingList.contains(viewedUserId)) {
                followingList.remove(viewedUserId)
                followersList.remove(currentUserId)
            } else {
                followingList.add(viewedUserId)
                followersList.add(currentUserId)
            }

            transaction.update(currentUserRef, "following", followingList)
            transaction.update(viewedUserRef, "followers", followersList)
        }.addOnSuccessListener {
            loadFollowCounts(currentUserId) // Refresh UI immediately after following/unfollowing
            Toast.makeText(requireContext(), "Follow status updated!", Toast.LENGTH_SHORT).show()
        }
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
