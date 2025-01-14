package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DettaglioGiocatoreActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var giocatore: Giocatore
    private lateinit var giocatoreId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_dettaglio_giocatore)
        setupBottomNavigation(R.id.nav_rosa)

        // Inizializza Firebase Database
        database = Firebase.database.reference

        // Ottieni l'ID del giocatore e i dati dall'intent
        giocatoreId = intent.getStringExtra("GIOCATORE_ID") ?: ""
        giocatore = intent.getSerializableExtra("GIOCATORE") as Giocatore

        // Mostra i dettagli del giocatore
        mostraDettagliGiocatore()

        // Riferimenti ai pulsanti
        val buttonModifica = findViewById<Button>(R.id.buttonModificaGiocatore)
        val buttonElimina = findViewById<Button>(R.id.buttonEliminaGiocatore)
        val buttonBack = findViewById<ImageButton>(R.id.back_button)

        buttonModifica.setOnClickListener {
            val intent = Intent(this, AggiungiGiocatoreActivity::class.java).apply {
                putExtra("GIOCATORE_ID", giocatoreId) // Passa l'ID del giocatore
                putExtra("GIOCATORE", giocatore) // Passa l'oggetto Giocatore
            }
            startActivity(intent)
        }

        buttonBack.setOnClickListener{
            finish()
        }

        // Gestisci il click sul pulsante "Elimina Giocatore"
        buttonElimina.setOnClickListener {
            eliminaGiocatore(giocatoreId)
        }
    }

    private fun mostraDettagliGiocatore() {
        val textViewNome = findViewById<TextView>(R.id.textViewNome)
        val textViewCognome = findViewById<TextView>(R.id.textViewCognome)
        val textViewEta = findViewById<TextView>(R.id.textViewEta)
        val textViewRuolo = findViewById<TextView>(R.id.textViewRuolo)

        textViewNome.text = giocatore.nome
        textViewCognome.text = giocatore.cognome
        textViewEta.text = "${giocatore.eta}"
        textViewRuolo.text = giocatore.ruolo
    }

    private fun eliminaGiocatore(giocatoreId: String) {
        database.child("Giocatori").child(giocatoreId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Giocatore eliminato con successo", Toast.LENGTH_SHORT).show()
                finish() // Chiudi l'Activity dopo l'eliminazione
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore durante l'eliminazione del giocatore", Toast.LENGTH_SHORT).show()
            }
    }
}