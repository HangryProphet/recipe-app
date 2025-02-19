package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var searchView: androidx.appcompat.widget.SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        searchView = findViewById(R.id.searchView) as androidx.appcompat.widget.SearchView

        // Highlight the current tab
        bottomNavigationView.selectedItemId = R.id.nav_home

        // Handle navigation clicks
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_bookmark -> navigateTo(BookmarkActivity::class.java, R.anim.slide_in_right, R.anim.slide_out_left)
                R.id.nav_upload -> navigateTo(UploadActivity::class.java, R.anim.slide_in_right, R.anim.slide_out_left)
                R.id.nav_calculator -> navigateTo(CalculatorActivity::class.java, R.anim.slide_in_right, R.anim.slide_out_left)
                R.id.nav_profile -> navigateTo(UserActivity::class.java, R.anim.slide_in_right, R.anim.slide_out_left)
                R.id.nav_home -> true // Already in HomeActivity
                else -> false
            }
        }

        searchView.queryHint = "Search for recipes..."

        // ✅ Remove default SearchView icon
        searchView.setIconifiedByDefault(false)

        // ✅ Make whole SearchView clickable, including hint
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_stay) // Slide Up Effect
                searchView.clearFocus() // Prevents accidental double focus issues
            }
        }
    }

    private fun navigateTo(targetActivity: Class<*>, enterAnim: Int, exitAnim: Int): Boolean {
        startActivity(Intent(this, targetActivity))
        overridePendingTransition(enterAnim, exitAnim)
        finish()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right) // Apply reverse animation
    }
}
