package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference

class DettaglioGiornoActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var allenamento: Allenamento
    private lateinit var allenamentoId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_giorno)
        setupBottomNavigation(R.id.nav_calendar)


        val cardDate: TextView = findViewById(R.id.card_date)
        val backbutton: ImageButton = findViewById(R.id.back_button)

        // Ottieni il giorno selezionato dall'Intent
        val selectedDay = intent.getStringExtra("selectedDay")
        val selectedMonth = intent.getIntExtra("selectedMonth", -1)
        val selectedYear = intent.getIntExtra("selectedYear", -1)

        val date = "$selectedDay/${selectedMonth+1}/$selectedYear"
        cardDate.text = date

        backbutton.setOnClickListener{
            finish()
        }



    }
}