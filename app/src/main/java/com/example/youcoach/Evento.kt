package com.example.youcoach

data class Evento(
    val minutaggio: String = "",
    val nomeEvento: String = "",
    val nomeGiocatore: String = "",
    val tipoGol: String? = null,
    val corpoGol: String? = null,
    val tipoTiro: String? = null,
    val tipoInfortunio: String? = null,
    val giocatoreCambio: String? = null,
    val falloFatto: Boolean? = null,
    val tipoGiallo: String? = null,
    val tipoRosso: String? = null
)
