package com.example.cookpal

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath

class FollowingActivity : AppCompatActivity() {

    private lateinit var searchField: EditText
    private lateinit var backButton: ImageView
    private lateinit var followingRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter

    private val db = FirebaseFirestore.getInstance()
    private var userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_following)

        // Initialize views
        searchField = findViewById(R.id.searchField)
        backButton = findViewById(R.id.backButton)
        followingRecyclerView = findViewById(R.id.followingRecyclerView)

        // Set up RecyclerView
        followingRecyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(userList) { selectedUser ->
            // TODO: Open selected user's profile
        }
        followingRecyclerView.adapter = userAdapter

        // Get the user ID from intent
        val userId = intent.getStringExtra("USER_ID") ?: return

        // Load following users
        loadFollowing(userId)

        // Handle back button click
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadFollowing(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val followingIds = document.get("following") as? List<String> ?: emptyList()

                if (followingIds.isNotEmpty()) {
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), followingIds)
                        .get()
                        .addOnSuccessListener { documents ->
                            val fetchedUsers = mutableListOf<User>()
                            for (doc in documents) {
                                val user = User(
                                    userId = doc.id,
                                    username = doc.getString("username") ?: "Unknown"
                                )
                                fetchedUsers.add(user)
                            }
                            userAdapter.updateList(fetchedUsers)
                        }
                }
            }
    }
}
