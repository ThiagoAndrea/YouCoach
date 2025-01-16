package com.example.youcoach

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class AggiungiAllenamentoActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private var allenamentoId: String? = null // ID dell'allenamento (null se stiamo aggiungendo un nuovo allenamento)
    private var selectedDate: String = "" // Data selezionata nel formato "YYYY-MM-DD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_allenamento)
        setupBottomNavigation(R.id.nav_calendar)

        database = Firebase.database.reference

        // Riferimenti ai campi di input
        val editTextData = findViewById<EditText>(R.id.editTextData)
        val editTextOrarioInizio = findViewById<EditText>(R.id.editTextOrarioInizio)
        val editTextOrarioFine = findViewById<EditText>(R.id.editTextOrarioFine)
        val buttonAggiungi = findViewById<Button>(R.id.buttonAggiungiAllenamento)
        val buttonBack = findViewById<ImageButton>(R.id.back_button)

        buttonBack.setOnClickListener {
            finish()
        }

        // Gestione della selezione della data
        editTextData.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Formatta la data come "YYYY-MM-DD" per il database
                    selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    // Mostra la data nel formato "DD/MM/YYYY" all'utente
                    val dataFormattata = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    editTextData.setText(dataFormattata)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        // Gestione della selezione dell'orario di inizio
        editTextOrarioInizio.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                    editTextOrarioInizio.setText(time)
                },
                hour,
                minute,
                true // Formato 24 ore
            )
            timePickerDialog.show()
        }

        // Gestione della selezione dell'orario di fine
        editTextOrarioFine.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                    editTextOrarioFine.setText(time)
                },
                hour,
                minute,
                true // Formato 24 ore
            )
            timePickerDialog.show()
        }

        // Verifica se stiamo modificando un allenamento esistente
        allenamentoId = intent.getStringExtra("ALLENAMENTO_ID")
        if (allenamentoId != null) {
            // Modalità modifica: popola i campi con i dati esistenti
            val allenamento = intent.getSerializableExtra("ALLENAMENTO") as Allenamento
            editTextOrarioInizio.setText(allenamento.orarioInizio)
            editTextOrarioFine.setText(allenamento.orarioFine)
            selectedDate = intent.getStringExtra("DATA") ?: "" // Recupera la data dall'intent

            // Cambia il testo del pulsante
            buttonAggiungi.text = "Modifica Allenamento"
        }

        // Gestisci il click sul pulsante "Aggiungi/Modifica Allenamento"
        buttonAggiungi.setOnClickListener {
            val orarioInizio = editTextOrarioInizio.text.toString().trim()
            val orarioFine = editTextOrarioFine.text.toString().trim()

            if (selectedDate.isEmpty() || orarioInizio.isEmpty() || orarioFine.isEmpty()) {
                // Mostra un messaggio di errore se i campi sono vuoti
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            } else {
                if (allenamentoId != null) {
                    modificaAllenamento(selectedDate, allenamentoId!!, orarioInizio, orarioFine)
                } else {
                    aggiungiAllenamento(selectedDate, orarioInizio, orarioFine)
                }
            }
        }
    }

    // Funzione per aggiungere un nuovo allenamento
    private fun aggiungiAllenamento(data: String, orarioInizio: String, orarioFine: String) {
        val allenamentoId = database.child("Allenamenti").child(data).push().key

        if (allenamentoId != null) {
            val allenamento = Allenamento(allenamentoId, orarioInizio, orarioFine)
            database.child("Allenamenti").child(data).child(allenamentoId).setValue(allenamento)
                .addOnSuccessListener {
                    Toast.makeText(this, "Allenamento aggiunto con successo", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, CalendarActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore durante l'aggiunta dell'allenamento", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Funzione per modificare un allenamento esistente
    private fun modificaAllenamento(data: String, allenamentoId: String, orarioInizio: String, orarioFine: String) {
        val allenamento = Allenamento(allenamentoId, orarioInizio, orarioFine)
        database.child("Allenamenti").child(data).child(allenamentoId).setValue(allenamento)
            .addOnSuccessListener {
                Toast.makeText(this, "Allenamento modificato con successo", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, CalendarActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore durante la modifica dell'allenamento", Toast.LENGTH_SHORT).show()
            }
    }
}