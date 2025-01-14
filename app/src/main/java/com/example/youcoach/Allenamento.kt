package com.example.youcoach

import java.io.Serializable

data class Allenamento(
    val id: String = "",
    val data: String = "",
    val orarioInizio: String = "",
    val orarioFine: String = ""
) : Serializable