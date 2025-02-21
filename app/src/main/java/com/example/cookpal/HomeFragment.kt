package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var greetingText: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        searchView = view.findViewById(R.id.searchView)
        greetingText = view.findViewById(R.id.greetingText) // ✅ Add this line

        loadUserData()

        searchView.queryHint = "Search for recipes..."
        searchView.setIconifiedByDefault(false)

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val intent = Intent(requireActivity(), SearchActivity::class.java)
                startActivity(intent)
                requireActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_stay) // Slide Up Effect
                searchView.clearFocus() // Prevents accidental double focus issues
            }
        }

        return view
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "User"
                    greetingText.text = "HELLO, $username"
                }
            }
            .addOnFailureListener {
                greetingText.text = "HELLO, User"
            }
    }
}
