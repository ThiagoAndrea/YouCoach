package com.example.youcoach

import java.io.Serializable

data class Allenamento(
    val id: String = "",
    val orarioInizio: String = "",
    val orarioFine: String = "",
    val obiettivi: List<String> = emptyList(),
    val presenze: Map<String, Int> = emptyMap()
) : Serializable