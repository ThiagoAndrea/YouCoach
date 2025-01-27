package com.example.youcoach

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
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

    private fun getGiocatoriFromFirebase(onSuccess: (Map<Giocatore, String>) -> Unit) {
        val giocatoriRef = database.child("Giocatori")
        giocatoriRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val giocatori = mutableMapOf<Giocatore, String>()
                for (giocatoreSnapshot in snapshot.children) {
                    val id = giocatoreSnapshot.key ?: continue
                    val nome = giocatoreSnapshot.child("nome").getValue(String::class.java) ?: ""
                    val cognome = giocatoreSnapshot.child("cognome").getValue(String::class.java) ?: ""
                    val giocatore = Giocatore(id, nome, cognome)
                    giocatori[giocatore] = id
                }
                onSuccess(giocatori)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AggiungiAllenamentoActivity, "Errore nel recupero dei giocatori", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun aggiungiAllenamento(data: String, orarioInizio: String, orarioFine: String) {
        getGiocatoriFromFirebase { giocatori ->
            // Crea una mappa delle presenze con chiavi di tipo String (ID del giocatore)
            val presenze = giocatori.mapValues { (giocatore, _) ->
                0 // Stato predefinito: "Assente"
            }.mapKeys { it.key.id } // Usa l'ID del giocatore come chiave

            val allenamentoId = database.child("Allenamenti").child(data).push().key

            if (allenamentoId != null) {
                val allenamento = Allenamento(allenamentoId, orarioInizio, orarioFine, obiettivi = emptyList(), presenze)
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
    }

    // Funzione per modificare un allenamento esistente
    private fun modificaAllenamento(data: String, allenamentoId: String, orarioInizio: String, orarioFine: String) {
        // Recupera i giocatori dalla rosa
        getGiocatoriFromFirebase { giocatori ->
            // Recupera l'allenamento esistente per mantenere le presenze
            val allenamentoRef = database.child("Allenamenti").child(data).child(allenamentoId)
            allenamentoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Recupera la mappa delle presenze esistente (se presente)
                        val presenzeEsistenti = snapshot.child("presenze").getValue(object : GenericTypeIndicator<Map<String, Int>>() {})
                            ?: emptyMap()

                        // Crea una nuova mappa delle presenze con tutti i giocatori
                        val presenze = giocatori.mapValues { (giocatore, _) ->
                            // Mantieni lo stato esistente se il giocatore è già presente, altrimenti imposta "Assente" (0)
                            presenzeEsistenti[giocatore.id] ?: 0
                        }.mapKeys { it.key.id } // Usa l'ID del giocatore come chiave

                        // Crea l'oggetto Allenamento aggiornato
                        val allenamento = Allenamento(
                            id = allenamentoId,
                            orarioInizio = orarioInizio,
                            orarioFine = orarioFine,
                            obiettivi = snapshot.child("obiettivi").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList(),
                            presenze = presenze
                        )

                        // Salva l'allenamento aggiornato in Firebase
                        allenamentoRef.setValue(allenamento)
                            .addOnSuccessListener {
                                Toast.makeText(this@AggiungiAllenamentoActivity, "Allenamento modificato con successo", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@AggiungiAllenamentoActivity, CalendarActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("AggiungiAllenamentoActivity", "Errore durante la modifica dell'allenamento", e)
                                Toast.makeText(this@AggiungiAllenamentoActivity, "Errore durante la modifica dell'allenamento", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: Exception) {
                        Log.e("AggiungiAllenamentoActivity", "Errore in onDataChange: ${e.message}", e)
                        Toast.makeText(this@AggiungiAllenamentoActivity, "Errore durante la modifica dell'allenamento", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AggiungiAllenamentoActivity", "Errore nel recupero dell'allenamento: ${error.message}")
                    Toast.makeText(this@AggiungiAllenamentoActivity, "Errore nel recupero dell'allenamento", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}