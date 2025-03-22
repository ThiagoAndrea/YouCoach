package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DettaglioGiocatoreActivity : BaseActivity() {

    private lateinit var giocatore: Giocatore
    private lateinit var giocatoreId: String

    private val db = DatabaseManager()
    private lateinit var modificaGiocatoreLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_dettaglio_giocatore)
        setupBottomNavigation(R.id.nav_rosa)

        // Inizializza l'ActivityResultLauncher
        modificaGiocatoreLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Ricarica i dettagli del giocatore
                caricaDettagliGiocatore()
            }
        }

        giocatoreId = intent.getStringExtra("GIOCATORE_ID") ?: ""
        giocatore = intent.getSerializableExtra("GIOCATORE") as Giocatore

        mostraDettagliGiocatore()

        val buttonModifica = findViewById<ImageButton>(R.id.modificagiocatore_button)
        val buttonElimina = findViewById<ImageButton>(R.id.eliminagiocatore_button)
        val buttonBack = findViewById<ImageButton>(R.id.back_button)

        buttonModifica.setOnClickListener {
            val intent = Intent(this, AggiungiGiocatoreActivity::class.java).apply {
                putExtra("GIOCATORE_ID", giocatoreId)
                putExtra("GIOCATORE", giocatore)
            }
            modificaGiocatoreLauncher.launch(intent)
        }

        buttonBack.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

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

    private fun caricaDettagliGiocatore() {
        db.getGiocatore(giocatoreId) { giocatore ->
            if (giocatore != null) {
                this.giocatore = giocatore
                mostraDettagliGiocatore()
            } else {
                Toast.makeText(this, "Errore nel caricamento dei dettagli del giocatore", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminaGiocatore(giocatoreId: String) {
        db.eliminaGiocatore(giocatoreId) { success, message ->
            if (success) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}