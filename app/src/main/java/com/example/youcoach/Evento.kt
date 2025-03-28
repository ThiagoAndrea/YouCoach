package com.example.youcoach

import java.io.Serializable

data class Evento(
    var idEvento: String="",
    val minutaggio: String ="",
    val nomeEvento: String = "",
    val nomeGiocatore: String = "",
    val squadra: Boolean = true,
    val dettagli: Map<String, Any?> = emptyMap()
): Serializable
