package com.example.youcoach

import java.io.Serializable

data class Partita(
    val id: String = "",
    val orario: String = "",
    val data: String = "",
    val luogo: String = "",
    val avversario: String = "",
    val risultato: String = "",
    val competizione: String = "",
    val numero_tempi: Int = 0,
    val minuti_per_tempo: Int = 0,
    val numero_calciatori: Int = 0,
    val casa: Boolean = false,
    val giocata: Boolean = false,
    val modulo: String = "",
    val convocati: Map<String, Boolean> = emptyMap(),
    val titolari: Map<String, String> = emptyMap(),


) : Serializable