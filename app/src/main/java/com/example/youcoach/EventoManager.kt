package com.example.youcoach

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.database

class EventoManager {
    private val database = Firebase.database.reference

    fun registraEvento(idPartita: String, data: String, idGiocatore: String, tipoEvento: String, dettagli: String? = null) {
        val eventiRef = database.child("Partite").child(data).child(idPartita).child("Eventi")
        val eventoId = eventiRef.push().key ?: return

        val evento = hashMapOf(
            "idGiocatore" to idGiocatore,
            "tipo" to tipoEvento,
            "dettagli" to (dettagli ?: ""),
            "timestamp" to System.currentTimeMillis()
        )

        eventiRef.child(eventoId).setValue(evento)
            .addOnSuccessListener {
                Log.d("EventoManager", "Evento registrato con successo: $evento")
            }
            .addOnFailureListener { e ->
                Log.e("EventoManager", "Errore nella registrazione dell'evento", e)
            }
    }
}
