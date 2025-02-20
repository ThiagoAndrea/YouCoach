package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AggiungiGiocatoreActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private var giocatoreId: String? = null // ID del giocatore (null se stiamo aggiungendo un nuovo giocatore)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_giocatore)
        setupBottomNavigation(R.id.nav_rosa)

        database = Firebase.database.reference

        // Riferimenti ai campi di input
        val editTextNome = findViewById<EditText>(R.id.editTextNome)
        val editTextCognome = findViewById<EditText>(R.id.editTextCognome)
        val editTextEta = findViewById<EditText>(R.id.editTextEta)
        val spinnerRuolo = findViewById<Spinner>(R.id.spinnerRuolo)
        val buttonAggiungi = findViewById<Button>(R.id.buttonAggiungiGiocatore)
        val buttonBack = findViewById<ImageButton>(R.id.back_button)

        // Popola lo spinner con i ruoli disponibili
        val ruoli = listOf("Portiere", "Difensore", "Centrocampista", "Attaccante")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ruoli)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRuolo.adapter = adapter

        buttonBack.setOnClickListener{
            finish()
        }

        // Verifica se stiamo modificando un giocatore esistente
        giocatoreId = intent.getStringExtra("GIOCATORE_ID")
        if (giocatoreId != null) {
            // Modalità modifica: popola i campi con i dati esistenti
            val giocatore = intent.getSerializableExtra("GIOCATORE") as Giocatore
            editTextNome.setText(giocatore.nome)
            editTextCognome.setText(giocatore.cognome)
            editTextEta.setText(giocatore.eta.toString())
            // Seleziona il ruolo corretto nello spinner
            val index = ruoli.indexOf(giocatore.ruolo)
            if (index >= 0) {
                spinnerRuolo.setSelection(index)
            }
            // Cambia il testo del pulsante
            buttonAggiungi.text = "Modifica Giocatore"
        }

        // Gestisci il click sul pulsante
        buttonAggiungi.setOnClickListener {
            val nome = editTextNome.text.toString().trim()
            val cognome = editTextCognome.text.toString().trim()
            val eta = editTextEta.text.toString().trim()
            // Legge il valore selezionato dallo spinner
            val ruolo = spinnerRuolo.selectedItem.toString()

            if (nome.isEmpty() || cognome.isEmpty() || eta.isEmpty() || ruolo.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            } else {
                if (giocatoreId != null) {
                    // Modalità modifica: aggiorna il giocatore esistente
                    modificaGiocatore(giocatoreId!!, nome, cognome, eta.toInt(), ruolo)
                } else {
                    // Modalità aggiunta: aggiungi un nuovo giocatore
                    aggiungiGiocatore(nome, cognome, eta.toInt(), ruolo)
                }
            }
        }
    }

    private fun aggiungiGiocatore(nome: String, cognome: String, eta: Int, ruolo: String) {
        val giocatoreId = database.child("Giocatori").push().key

        if (giocatoreId != null) {
            val giocatore = Giocatore(giocatoreId, nome, cognome, eta, ruolo)
            database.child("Giocatori").child(giocatoreId).setValue(giocatore)
                .addOnSuccessListener {
                    Toast.makeText(this, "Giocatore aggiunto con successo", Toast.LENGTH_SHORT).show()
                    finish() // Chiudi l'Activity dopo l'aggiunta
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore durante l'aggiunta del giocatore", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun modificaGiocatore(giocatoreId: String, nome: String, cognome: String, eta: Int, ruolo: String) {
        val giocatore = Giocatore(giocatoreId, nome, cognome, eta, ruolo)
        database.child("Giocatori").child(giocatoreId).setValue(giocatore)
            .addOnSuccessListener {
                Toast.makeText(this, "Giocatore modificato con successo", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, RosaActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent) // Chiudi l'Activity dopo la modifica
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore durante la modifica del giocatore", Toast.LENGTH_SHORT).show()
            }
    }
}
