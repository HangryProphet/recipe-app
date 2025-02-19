package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth

class UserActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var settingsButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var profileChipGroup: ChipGroup
    private lateinit var chipRecipes: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        settingsButton = findViewById(R.id.settingsButton)
        profileChipGroup = findViewById(R.id.profileChipGroup)
        chipRecipes = findViewById(R.id.chipRecipes)

        // Set "Recipes" chip as default selected
        chipRecipes.isChecked = true

        // Highlight the current tab
        bottomNavigationView.selectedItemId = R.id.nav_profile

        // Handle navigation clicks
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateTo(HomeActivity::class.java, R.anim.slide_in_left, R.anim.slide_out_right)
                R.id.nav_bookmark -> navigateTo(BookmarkActivity::class.java, R.anim.slide_in_left, R.anim.slide_out_right)
                R.id.nav_upload -> navigateTo(UploadActivity::class.java, R.anim.slide_in_left, R.anim.slide_out_right)
                R.id.nav_calculator -> navigateTo(CalculatorActivity::class.java, R.anim.slide_in_left, R.anim.slide_out_right)
                R.id.nav_profile -> true // Already in UserActivity
                else -> false
            }
        }

        // Show settings menu when clicking the three dots
        settingsButton.setOnClickListener {
            showSettingsMenu(settingsButton)
        }
    }

    private fun navigateTo(targetActivity: Class<*>, enterAnim: Int, exitAnim: Int): Boolean {
        startActivity(Intent(this, targetActivity))
        overridePendingTransition(enterAnim, exitAnim)
        finish()
        return true
    }

    private fun showSettingsMenu(anchor: ImageView) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.user_profile_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_edit_profile -> {
                    // TODO: Navigate to Edit Profile (implement later)
                    true
                }
                R.id.menu_settings -> {
                    // TODO: Navigate to Settings Activity (implement later)
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

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
