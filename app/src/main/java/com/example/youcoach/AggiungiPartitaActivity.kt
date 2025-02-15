package com.example.youcoach

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class AggiungiPartitaActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private var partitaId: String? = null
    private var selectedDate: String = ""

    // UI Components
    private lateinit var editTextAvversario: EditText
    private lateinit var editTextOrario: EditText
    private lateinit var editTextLuogo: EditText
    private lateinit var editTextRisultato: EditText
    private lateinit var spinnerCompetizione: Spinner
    private lateinit var switchCasaTrasferta: SwitchCompat
    private lateinit var editTextNumTempi: EditText
    private lateinit var editTextNumGiocatori: EditText
    private lateinit var editTextMinTempi: EditText
    private lateinit var editTextModulo: EditText
    private lateinit var editTextData: EditText
    private lateinit var buttonAggiungi: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_partita)
        setupBottomNavigation(R.id.nav_calendar)

        database = FirebaseDatabase.getInstance().reference

        initUI()
        setupButtonListeners()
    }

    private fun initUI() {
        editTextAvversario = findViewById(R.id.editTextAvversario)
        editTextOrario = findViewById(R.id.editTextOrario)
        editTextLuogo = findViewById(R.id.editTextLuogo)
        spinnerCompetizione = findViewById(R.id.spinnerCompetizione)
        switchCasaTrasferta = findViewById(R.id.switchCasa)
        editTextNumTempi = findViewById(R.id.editNumTempi)
        editTextNumGiocatori = findViewById(R.id.editNumGiocatori)
        editTextMinTempi = findViewById(R.id.editMinPerTempo)
        editTextData = findViewById(R.id.editTextData)
        buttonAggiungi = findViewById(R.id.buttonAggiungiPartita)

        // Popola lo Spinner con le opzioni
        val competizioni = arrayOf("Amichevole", "Coppa", "Campionato")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, competizioni)
        spinnerCompetizione.adapter = adapter
    }

    private fun setupButtonListeners() {
        editTextData.setOnClickListener { selezionaData() }
        editTextOrario.setOnClickListener { selezionaOrario() }

        buttonAggiungi.setOnClickListener {
            salvaPartita()
        }
    }

    private fun selezionaData() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                editTextData.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun selezionaOrario() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                editTextOrario.setText(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun salvaPartita() {
        val avversario = editTextAvversario.text.toString().trim()
        val orario = editTextOrario.text.toString().trim()
        val luogo = editTextLuogo.text.toString().trim()
        val competizione = editCompetizione.selectedItem.toString()
        val casa = editCasaTrasferta.isChecked // Recupera il valore del SwitchCompat
        val numTempi = editTextNumTempi.text.toString().trim().toIntOrNull() ?: 0
        val numGiocatori = editTextNumGiocatori.text.toString().trim().toIntOrNull() ?: 0
        val minTempi = editTextMinTempi.text.toString().trim().toIntOrNull() ?: 0

        if (selectedDate.isEmpty() || avversario.isEmpty() || orario.isEmpty() ||
            luogo.isEmpty() || numTempi == 0 || numGiocatori == 0 || minTempi == 0) {
            Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            return
        }

        // Controlliamo il riferimento a Firebase
        val partitaId = database.child("Partite").child(selectedDate).push().key
        if (partitaId == null) {
            Toast.makeText(this, "Errore nella generazione dell'ID partita", Toast.LENGTH_SHORT).show()
            return
        }

        val partita = Partita(
            id = partitaId,
            orario = orario,
            luogo = luogo,
            avversario = avversario,
            competizione = competizione,
            numero_tempi = numTempi,
            minuti_per_tempo = minTempi,
            numero_calciatori = numGiocatori,
            casa = casa,
            modulo = "", // Aggiungi il valore corretto se necessario
            convocati = emptyMap(),
            titolari = emptyMap()
        )

        // Debug: stampiamo i dati per vedere se sono validi
        println("Sto salvando la partita: $partita")

        database.child("Partite").child(selectedDate).child(partitaId)
            .setValue(partita)
            .addOnSuccessListener {
                Toast.makeText(this, "Partita aggiunta con successo", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore durante l'aggiunta: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace() // Log dell'errore
            }
    }



}
