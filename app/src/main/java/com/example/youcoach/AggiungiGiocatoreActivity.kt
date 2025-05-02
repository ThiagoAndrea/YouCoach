package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AggiungiGiocatoreActivity : BaseActivity() {

    private val db = DatabaseManager()
    private var giocatoreId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_giocatore)
        setupBottomNavigation(R.id.nav_rosa)

        val editTextNome = findViewById<EditText>(R.id.editTextNome)
        val editTextCognome = findViewById<EditText>(R.id.editTextCognome)
        val editTextEta = findViewById<EditText>(R.id.editTextEta)
        val spinnerRuolo = findViewById<Spinner>(R.id.spinnerRuolo)
        val buttonAggiungi = findViewById<Button>(R.id.buttonAggiungiGiocatore)
        val buttonBack = findViewById<ImageButton>(R.id.back_button)

        val ruoli = listOf("Portiere", "Difensore", "Centrocampista", "Attaccante")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ruoli)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRuolo.adapter = adapter

        buttonBack.setOnClickListener {
            finish()
        }

        giocatoreId = intent.getStringExtra("GIOCATORE_ID")
        if (giocatoreId != null) {
            val giocatore = intent.getSerializableExtra("GIOCATORE") as Giocatore
            editTextNome.setText(giocatore.nome)
            editTextCognome.setText(giocatore.cognome)
            editTextEta.setText(giocatore.eta.toString())
            val index = ruoli.indexOf(giocatore.ruolo)
            if (index >= 0) {
                spinnerRuolo.setSelection(index)
            }
            buttonAggiungi.text = "Modifica Giocatore"
        }

        buttonAggiungi.setOnClickListener {
            val nome = editTextNome.text.toString().trim()
            val cognome = editTextCognome.text.toString().trim()
            val eta = editTextEta.text.toString().trim()
            val ruolo = spinnerRuolo.selectedItem.toString()

            if (nome.isEmpty() || cognome.isEmpty() || eta.isEmpty() || ruolo.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            } else {
                if (giocatoreId != null) {
                    // Modifica giocatore esistente
                    db.modificaGiocatore(giocatoreId!!, nome, cognome, eta.toInt(), ruolo) { success, message ->
                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK) // Imposta il risultato come OK
                            finish() // Chiudi l'activity
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Aggiungi nuovo giocatore
                    db.aggiungiGiocatore(nome, cognome, eta.toInt(), ruolo) { success, message ->
                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK) // Imposta il risultato come OK
                            finish() // Chiudi l'activity
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}