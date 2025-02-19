package com.example.cookpal

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.AppCompatImageView

class SearchActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var backButton: AppCompatImageView
    private lateinit var filterButton: AppCompatImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize Views
        searchView = findViewById(R.id.searchView)
        backButton = findViewById(R.id.backButton)
        filterButton = findViewById(R.id.filterButton)

        // Automatically open keyboard for search input
        searchView.isIconified = false
        searchView.requestFocus()

        // Remove the magnifying glass icon
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        val searchIcon = searchView.findViewById<AppCompatImageView>(androidx.appcompat.R.id.search_mag_icon)
        val closeIcon = searchView.findViewById<AppCompatImageView>(androidx.appcompat.R.id.search_close_btn)

        searchIcon?.setImageDrawable(null) // Hide the search icon
        closeIcon?.setImageDrawable(null) // Hide the close icon when no text is entered

        searchEditText.hint = "Search recipe..." // Set custom hint
        searchEditText.textSize = 16f // Adjust text size

        // Handle Back Button Click
        backButton.setOnClickListener {
            finish() // Close SearchActivity
            overridePendingTransition(0, R.anim.slide_out_down) // Corrected Animation
        }

        // Handle Search Query Submission
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus() // Hide keyboard when submitting search
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    // Handle back button (Hardware Back Button)
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.slide_out_down)
    }
}
