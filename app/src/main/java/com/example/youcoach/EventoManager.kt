package com.example.youcoach

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.database

class EventoManager {
    private val database = Firebase.database.reference

    fun registraEvento(idPartita: String, data: String, minutaggio: String,idGiocatore: String, nomeEvento: String, squadra: Boolean, dettagliEvento: Map<String, Any?> = emptyMap()) {
        val eventiRef = database.child("Partite").child(data).child(idPartita).child("eventi")
        val eventoId = eventiRef.push().key ?: return

        val eventoMap = mapOf(
            "idEvento" to eventoId,
            "minutaggio" to minutaggio,
            "nomeEvento" to nomeEvento,
            "nomeGiocatore" to idGiocatore,
            "squadra" to squadra,
            "dettagli" to dettagliEvento
        )

        eventiRef.child(eventoId).setValue(eventoMap)
    }

}
