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
    private lateinit var spinnerCompetizione: Spinner
    private lateinit var switchCasaTrasferta: SwitchCompat
    private lateinit var editTextNumTempi: EditText
    private lateinit var editTextNumGiocatori: EditText
    private lateinit var editTextMinTempi: EditText
    private lateinit var editTextData: EditText
    private lateinit var buttonAggiungi: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_partita)
        setupBottomNavigation(R.id.nav_calendar)

        database = FirebaseDatabase.getInstance().reference

        initUI()
        checkIntentData() // Controlla se ci sono dati da precompilare
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

        val competizioni = arrayOf("Amichevole", "Coppa", "Campionato")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, competizioni)
        spinnerCompetizione.adapter = adapter
    }

    private fun checkIntentData() {
        partitaId = intent.getStringExtra("PARTITA_ID")
        val data = intent.getStringExtra("DATA")
        val orario = intent.getStringExtra("ORARIO")
        val avversario = intent.getStringExtra("AVVERSARIO")
        val luogo = intent.getStringExtra("LUOGO")
        val competizione = intent.getStringExtra("COMPETIZIONE")
        val casa = intent.getBooleanExtra("CASA", false)
        val numTempi = intent.getIntExtra("NUMERO_TEMPI", 0)
        val numGiocatori = intent.getIntExtra("NUMERO_GIOCATORI", 0)
        val minTempi = intent.getIntExtra("MINUTI_PER_TEMPO", 0)

        if (data != null) {
            selectedDate = data
            editTextData.setText(data.replace("-", "/"))
        }
        if (orario != null) editTextOrario.setText(orario)
        if (avversario != null) editTextAvversario.setText(avversario)
        if (luogo != null) editTextLuogo.setText(luogo)
        if (competizione != null) {
            val index = (spinnerCompetizione.adapter as ArrayAdapter<String>).getPosition(competizione)
            spinnerCompetizione.setSelection(index)
        }
        switchCasaTrasferta.isChecked = casa
        if (numTempi > 0) editTextNumTempi.setText(numTempi.toString())
        if (numGiocatori > 0) editTextNumGiocatori.setText(numGiocatori.toString())
        if (minTempi > 0) editTextMinTempi.setText(minTempi.toString())

        if (partitaId != null) {
            buttonAggiungi.text = "Modifica Partita"
        }
    }

    private fun setupButtonListeners() {
        editTextData.setOnClickListener { selezionaData() }
        editTextOrario.setOnClickListener { selezionaOrario() }
        buttonAggiungi.setOnClickListener { salvaPartita() }
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
        val competizione = spinnerCompetizione.selectedItem.toString()
        val casa = switchCasaTrasferta.isChecked
        val numTempi = editTextNumTempi.text.toString().trim().toIntOrNull() ?: 0
        val numGiocatori = editTextNumGiocatori.text.toString().trim().toIntOrNull() ?: 0
        val minTempi = editTextMinTempi.text.toString().trim().toIntOrNull() ?: 0

        if (selectedDate.isEmpty() || avversario.isEmpty() || orario.isEmpty() ||
            luogo.isEmpty() || numTempi == 0 || numGiocatori == 0 || minTempi == 0) {
            Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            return
        }

        val partita = Partita(
            id = partitaId ?: database.child("Partite").child(selectedDate).push().key!!,
            orario = orario,
            luogo = luogo,
            avversario = avversario,
            competizione = competizione,
            numero_tempi = numTempi,
            minuti_per_tempo = minTempi,
            numero_calciatori = numGiocatori,
            casa = casa,
            risultato = "",
            modulo = "",
            convocati = emptyMap(),
            titolari = emptyMap()
        )

        database.child("Partite").child(selectedDate).child(partita.id)
            .setValue(partita)
            .addOnSuccessListener {
                Toast.makeText(this, "Partita salvata con successo", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
