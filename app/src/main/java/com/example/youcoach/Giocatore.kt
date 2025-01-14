package com.example.youcoach
import java.io.Serializable

data class Giocatore(
    val id: String ="",
    val nome: String = "",
    val cognome: String = "",
    val eta: Int=0,
    val ruolo: String = ""
) : Serializable
