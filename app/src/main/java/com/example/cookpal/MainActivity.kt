package com.example.cookpal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private var currentTabIndex = 0 // Tracks the current fragment position

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Check if we're opening UploadFragment with recipe data
        if (intent.hasExtra("FRAGMENT_TO_LOAD") && intent.getStringExtra("FRAGMENT_TO_LOAD") == "UploadFragment") {
            openUploadFragmentWithData()
        } else {
            // Load default fragment (Home) if no specific fragment is requested
            if (savedInstanceState == null) {
                loadFragment(HomeFragment(), false)
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val newIndex = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_bookmark -> 1
                R.id.nav_upload -> 2
                R.id.nav_calculator -> 3
                R.id.nav_profile -> 4
                else -> return@setOnItemSelectedListener false
            }

            // Only change the fragment if it's different
            if (newIndex != currentTabIndex) {
                val isGoingRight = newIndex > currentTabIndex
                currentTabIndex = newIndex
                loadFragment(getFragmentForIndex(newIndex), true, isGoingRight)
            }

            true
        }
    }

    private fun loadFragment(fragment: Fragment, addAnimation: Boolean, isGoingRight: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()

        if (addAnimation) {
            transaction.setCustomAnimations(
                if (isGoingRight) R.anim.slide_in_right else R.anim.slide_in_left,  // Enter animation
                if (isGoingRight) R.anim.slide_out_left else R.anim.slide_out_right  // Exit animation
            )
        }

        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun getFragmentForIndex(index: Int): Fragment {
        return when (index) {
            0 -> HomeFragment()
            1 -> BookmarkFragment()
            2 -> UploadFragment()
            3 -> CalculatorFragment()
            4 -> UserFragment()
            else -> HomeFragment() // Default case
        }
    }

    /**
     * 🔥 Handles opening UploadFragment when "Edit Recipe" is clicked
     * in RecipeActivity
     */
    private fun openUploadFragmentWithData() {
        val bundle = Bundle().apply {
            putString("RECIPE_ID", intent.getStringExtra("RECIPE_ID"))
            putString("RECIPE_NAME", intent.getStringExtra("RECIPE_NAME"))
            putString("CUISINE_TYPE", intent.getStringExtra("CUISINE_TYPE"))
            putString("COOKING_TIME", intent.getStringExtra("COOKING_TIME"))
            putString("SHORT_DESCRIPTION", intent.getStringExtra("SHORT_DESCRIPTION"))
            putStringArrayList("INGREDIENTS", intent.getStringArrayListExtra("INGREDIENTS"))
            putStringArrayList("STEPS", intent.getStringArrayListExtra("STEPS"))
        }

        val uploadFragment = UploadFragment().apply { arguments = bundle }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, uploadFragment)
            .addToBackStack(null)
            .commit()
    }
}
