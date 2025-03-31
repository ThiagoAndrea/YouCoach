import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.youcoach.Giocatore
import com.example.youcoach.R

class PresenzeAdapter(
    private val giocatori: List<Giocatore>,
    private val presenze: MutableMap<String, Int>,
    private val onPresenzaUpdated: (String, Int) -> Unit
) : RecyclerView.Adapter<PresenzeAdapter.ViewHolder>() {

    init {
        for (giocatore in giocatori) {
            if (!presenze.containsKey(giocatore.id)) {
                presenze[giocatore.id] = 0
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_presenza, parent, false)
        return ViewHolder(view, onPresenzaUpdated)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val giocatore = giocatori[position]
        holder.bind(giocatore, presenze[giocatore.id] ?: 0)
    }

    override fun getItemCount() = giocatori.size

    class ViewHolder(itemView: View, private val onPresenzaUpdated: (String, Int) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val nomeGiocatore: TextView = itemView.findViewById(R.id.txt_giocatore)
        private val btnPresente: ImageButton = itemView.findViewById(R.id.btn_presenza)
        private val btnAssente: ImageButton = itemView.findViewById(R.id.btn_assenza)
        private val btnRitardo: ImageButton = itemView.findViewById(R.id.btn_ritardo)
        private val btnInfortunato: ImageButton = itemView.findViewById(R.id.btn_infortunio)

        fun bind(giocatore: Giocatore, stato: Int) {
            nomeGiocatore.text = "${giocatore.nome} ${giocatore.cognome}"
            aggiornaUI(stato)

            // Imposta i listener per i pulsanti
            btnPresente.setOnClickListener { aggiornaPresenza(giocatore.id, 0) }
            btnAssente.setOnClickListener { aggiornaPresenza(giocatore.id, 1) }
            btnRitardo.setOnClickListener { aggiornaPresenza(giocatore.id, 2) }
            btnInfortunato.setOnClickListener { aggiornaPresenza(giocatore.id, 3) }
        }

        private fun aggiornaPresenza(idGiocatore: String, stato: Int) {
            onPresenzaUpdated(idGiocatore, stato)
            aggiornaUI(stato)
        }

        private fun aggiornaUI(stato: Int) {
            btnPresente.setBackgroundResource(R.drawable.background_circle_base)
            btnAssente.setBackgroundResource(R.drawable.background_circle_base)
            btnRitardo.setBackgroundResource(R.drawable.background_circle_base)
            btnInfortunato.setBackgroundResource(R.drawable.background_circle_base)

            when (stato) {
                0 -> btnPresente.setBackgroundResource(R.drawable.background_circle_confirm)
                1 -> btnAssente.setBackgroundResource(R.drawable.background_circle_delete)
                2 -> btnRitardo.setBackgroundResource(R.drawable.background_circle_late)
                3 -> btnInfortunato.setBackgroundResource(R.drawable.background_circle_injured)
            }
        }
    }
}