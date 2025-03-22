package com.example.youcoach


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupBottomNavigation(selectedItemId: Int) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = selectedItemId

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is HomeActivity) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_rosa -> {
                    if (this !is RosaActivity) {
                        startActivity(Intent(this, RosaActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_stats -> {
                    if (this !is StatsActivity) {
                        startActivity(Intent(this, StatsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_calendar -> {
                    if (this !is CalendarActivity) {
                        startActivity(Intent(this, CalendarActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
}