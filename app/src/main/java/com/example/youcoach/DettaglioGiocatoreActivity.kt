package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class DettaglioGiocatoreActivity : BaseActivity() {

    private lateinit var giocatore: Giocatore
    private lateinit var giocatoreId: String
    private val db = DatabaseManager()
    private lateinit var modificaGiocatoreLauncher: ActivityResultLauncher<Intent>
    private lateinit var num_presenze: TextView
    private lateinit var num_ritardi: TextView
    private lateinit var num_assenze: TextView
    private lateinit var num_infortunati: TextView
    private lateinit var num_convocazioni: TextView
    private lateinit var num_titolari: TextView
    private lateinit var num_minuti_giocati: TextView
    private lateinit var num_gol: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_giocatore)
        setupBottomNavigation(R.id.nav_rosa)

        modificaGiocatoreLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                caricaDettagliGiocatore()
            }
        }

        giocatoreId = intent.getStringExtra("GIOCATORE_ID") ?: ""
        giocatore = intent.getSerializableExtra("GIOCATORE") as Giocatore

        setUI()
        caricaStatsAllenamenti()
        caricaStatsPartite()

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

    private fun setUI() {
        val textViewNome = findViewById<TextView>(R.id.textViewNome)
        val textViewCognome = findViewById<TextView>(R.id.textViewCognome)
        val textViewEta = findViewById<TextView>(R.id.textViewEta)
        val textViewRuolo = findViewById<TextView>(R.id.textViewRuolo)
        num_presenze = findViewById(R.id.num_presenze)
        num_ritardi = findViewById(R.id.num_ritardi)
        num_assenze = findViewById(R.id.num_assenze)
        num_infortunati = findViewById(R.id.num_infortunati)
        num_convocazioni = findViewById(R.id.num_convocazioni)
        num_titolari = findViewById(R.id.num_titolari)
        num_minuti_giocati = findViewById(R.id.num_minuti_giocati)
        num_gol = findViewById(R.id.num_gol)

        textViewNome.text = giocatore.nome
        textViewCognome.text = giocatore.cognome
        textViewEta.text = "${giocatore.eta}"
        textViewRuolo.text = giocatore.ruolo
    }

    private fun caricaDettagliGiocatore() {
        db.getGiocatore(giocatoreId) { giocatore ->
            if (giocatore != null) {
                this.giocatore = giocatore
                setUI()
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

    private fun caricaStatsAllenamenti() {
        db.getStatsAllenamentiPerMese(giocatoreId, "Tutti") { presenze, assenze, ritardi, infortuni ->
            num_presenze.text = presenze.toString()
            num_ritardi.text = ritardi.toString()
            num_assenze.text = assenze.toString()
            num_infortunati.text = infortuni.toString()
        }
    }

    private fun caricaStatsPartite(){
        db.calcolaStatsPartitaGiocatoreBasic(giocatoreId){ convocazioni, titolare, min, gol ->
            num_convocazioni.text = convocazioni.toString()
            num_titolari.text = titolare.toString()
            num_minuti_giocati.text = min.toString()
            num_gol.text = gol.toString()
        }
    }

}