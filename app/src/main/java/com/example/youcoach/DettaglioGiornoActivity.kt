package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DettaglioGiornoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_giorno)
        setupBottomNavigation(R.id.nav_calendar)

        val txtDay: TextView = findViewById(R.id.txtDay)
        val btnTraining: Button = findViewById(R.id.btnTraining)
        val btnMatch: Button = findViewById(R.id.btnMatch)

        // Ottieni il giorno selezionato dall'Intent
        val selectedDay = intent.getStringExtra("selectedDay")
        txtDay.text = "Giorno: $selectedDay"

        // Gestisci il clic sui pulsanti
        btnTraining.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("eventType", "training")
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        btnMatch.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("eventType", "match")
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}