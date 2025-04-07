package com.example.youcoach

import androidx.recyclerview.widget.RecyclerView

object Utils {

    fun troncaTesto(testo: String?, maxLunghezza: Int): String {
        if (testo == null) {
            return ""
        }
        return if (testo.length > maxLunghezza) {
            testo.substring(0, maxLunghezza - 2) + "..."
        } else {
            testo
        }
    }

    fun parseMinuto(minutaggio: String?): Int {
        if (minutaggio.isNullOrBlank()) return 0
        val parts = minutaggio.split(":")
        val minuti = parts.getOrNull(0)?.toIntOrNull() ?: 0
        return  minuti + 1
    }

    fun aggiornaUIDopoSostituzione(
        partitaId: String,
        dataPartita: String,
        recyclerFormazione: RecyclerView,
        recyclerPanchina: RecyclerView,
        db: DatabaseManager
    ) {
        db.getFormazioneMap(partitaId, dataPartita) { titolari, panchina ->
            (recyclerFormazione.adapter as? FormazioneAdapter)?.updateFormazione(titolari)
            (recyclerPanchina.adapter as? PanchinaAdapter)?.updatePanchina(panchina)
        }
    }


}