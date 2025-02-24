package com.example.cookpal

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FollowersActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var backButton: ImageView
    private lateinit var followersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val displayedList = mutableListOf<User>() // Used for filtering results

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var viewedUserId: String // User whose followers we are viewing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)

        // Initialize views
        searchView = findViewById(R.id.searchField)
        backButton = findViewById(R.id.backButton)
        followersRecyclerView = findViewById(R.id.followersRecyclerView)

        // Get the user ID whose followers we are viewing
        viewedUserId = intent.getStringExtra("VIEWED_USER_ID") ?: auth.currentUser?.uid ?: ""

        // Set up RecyclerView
        followersRecyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(displayedList) { selectedUser ->
            goToUserProfile(selectedUser)
        }
        followersRecyclerView.adapter = userAdapter

        // Load followers
        loadFollowers()

        // Back button functionality
        backButton.setOnClickListener {
            finish() // Go back to the previous screen
        }

        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText ?: "")
                return true
            }
        })
    }

    private fun loadFollowers() {
        db.collection("users").document(viewedUserId).get()
            .addOnSuccessListener { document ->
                val followersList = document.get("followers") as? List<String> ?: emptyList()

                if (followersList.isEmpty()) {
                    Toast.makeText(this, "No followers found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userFetchTasks = followersList.map { userId ->
                    db.collection("users").document(userId).get()
                }

                userList.clear()
                displayedList.clear()

                userFetchTasks.forEach { task ->
                    task.addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            val user = User(
                                userId = userDoc.id,
                                username = userDoc.getString("username") ?: "Unknown"
                            )
                            userList.add(user)
                            displayedList.add(user)
                            userAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load followers.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterUsers(query: String) {
        displayedList.clear()
        displayedList.addAll(
            userList.filter { user ->
                user.username.contains(query, ignoreCase = true)
            }
        )
        userAdapter.notifyDataSetChanged()
    }

    private fun goToUserProfile(user: User) {
        val intent = android.content.Intent(this, UserProfileActivity::class.java)
        intent.putExtra("VIEWED_USER_ID", user.userId)
        startActivity(intent)
    }
}
