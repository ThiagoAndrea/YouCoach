import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.youcoach.Giocatore
import com.example.youcoach.R

class ConvocazioniAdapter(
    private val giocatori: List<Giocatore>,
    private val convocazioni: MutableMap<String, Boolean>,
    private val onConvocazioneUpdated: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ConvocazioniAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_convocazione, parent, false)
        return ViewHolder(view, onConvocazioneUpdated)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val giocatore = giocatori[position]
        holder.bind(giocatore, convocazioni[giocatore.id] ?: true)
    }

    override fun getItemCount() = giocatori.size

    class ViewHolder(itemView: View, private val onConvocazioneUpdated: (String, Boolean) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val nomeGiocatore: TextView = itemView.findViewById(R.id.txt_giocatore)
        private val switcher: SwitchCompat = itemView.findViewById(R.id.switchConvocazione)

        fun bind(giocatore: Giocatore, stato: Boolean) {
            nomeGiocatore.text = "${giocatore.nome} ${giocatore.cognome}"
            switcher.isChecked = stato

            switcher.setOnCheckedChangeListener { _, isChecked ->
                onConvocazioneUpdated(giocatore.id, isChecked)
            }
        }
    }
}
