package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class UploadActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_upload

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateTo(HomeActivity::class.java, R.anim.slide_in_left, R.anim.slide_out_right)
                R.id.nav_bookmark -> navigateTo(BookmarkActivity::class.java, R.anim.slide_in_left, R.anim.slide_out_right)
                R.id.nav_calculator -> navigateTo(CalculatorActivity::class.java, R.anim.slide_in_right, R.anim.slide_out_left)
                R.id.nav_profile -> navigateTo(UserActivity::class.java, R.anim.slide_in_right, R.anim.slide_out_left)
                else -> false
            }
        }
    }
    private fun navigateTo(targetActivity: Class<*>, enterAnim: Int, exitAnim: Int): Boolean {
        startActivity(Intent(this, targetActivity))
        overridePendingTransition(enterAnim, exitAnim)
        finish()
        return true
    }
}
