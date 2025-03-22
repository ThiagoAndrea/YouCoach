package com.example.youcoach

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AggiungiObiettivoDialogFragment(private val onObiettivoAggiunto: () -> Unit) : DialogFragment() {

    private val db = DatabaseManager()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.finestra_obiettivi, null)

        val editTextObiettivo = view.findViewById<EditText>(R.id.editTextObiettivo)
        val buttonSalva = view.findViewById<Button>(R.id.buttonSalvaObiettivo)

        builder.setView(view)
        val dialog = builder.create()

        buttonSalva.setOnClickListener {
            val nuovoObiettivo = editTextObiettivo.text.toString().trim()
            if (nuovoObiettivo.isNotEmpty()) {
                db.aggiungiObiettivo(nuovoObiettivo) { success, message ->
                    if (success) {
                        onObiettivoAggiunto()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }else {
                Toast.makeText(requireContext(), "Inserisci un obiettivo", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }


}
