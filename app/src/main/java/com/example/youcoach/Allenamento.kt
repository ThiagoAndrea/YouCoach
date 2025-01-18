package com.example.youcoach

import java.io.Serializable

data class Allenamento(
    val id: String = "",
    val orarioInizio: String = "",
    val orarioFine: String = "",
    val presenze: Map<Giocatore, Int>
) : Serializable