package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class StatsActivity : BaseActivity() {

    private lateinit var detailsAllenamenti: ImageButton
    private lateinit var detailsGiocatori: ImageButton
    private lateinit var detailsPartite: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        setupBottomNavigation(R.id.nav_stats)

        detailsAllenamenti = findViewById(R.id.btn_allenamenti)
        detailsAllenamenti.setOnClickListener {
            val intent = Intent(this, StatisticheAllenamentiActivity::class.java)
            startActivity(intent)
        }

        detailsPartite = findViewById(R.id.btn_partite)
        detailsPartite.setOnClickListener {
            val intent = Intent(this, StatistichePartiteActivity::class.java)
            startActivity(intent)
        }

        detailsGiocatori = findViewById(R.id.btn_giocatori)
        detailsGiocatori.setOnClickListener {
            val intent = Intent(this, StatisticheGiocatoriActivity::class.java)
            startActivity(intent)
        }


    }
}