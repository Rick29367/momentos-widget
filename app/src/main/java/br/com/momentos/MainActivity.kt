package br.com.momentos

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("momentos", Context.MODE_PRIVATE) }
    private lateinit var list: LinearLayout
    private val valueViews = mutableMapOf<String, TextView>()
    private val timerHandler = Handler(Looper.getMainLooper())
    private var selectedCategory = "Todos"
    private val categories = listOf("Todos", "Amor ❤️", "Trabalho 💼", "Metas 🎯", "Viagens ✈️", "Pessoal")
    private val bg = Color.rgb(13, 20, 31)
    private val panel = Color.rgb(29, 37, 49)
    private val gold = Color.rgb(218, 178, 112)
    private val muted = Color.rgb(170, 177, 188)
    private val secondTicker = object : Runnable {
        override fun run() {
            valueViews.forEach { (id, view) -> Moment.all(prefs).firstOrNull { it.id == id }?.let { view.text = describe(it) } }
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }

    private fun rounded(color: Int, stroke: Int = Color.TRANSPARENT, radius: Float = 20f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius; if (stroke != Color.TRANSPARENT) setStroke(2, stroke) }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 28, 22, 18); setBackgroundColor(bg) }
        root.addView(label("✦ MOMENTOS", 30f, Color.WHITE).apply { letterSpacing = .04f })
        root.addView(label("Seus momentos, organizados por perto.", 14f, muted).apply { setPadding(2, 5, 0, 22) })
        val add = TextView(this).apply {
            text = "＋  ADICIONAR DATA"; textSize = 15f; gravity = Gravity.CENTER; setTextColor(gold)
            setPadding(12, 15, 12, 15); background = rounded(Color.rgb(38, 39, 42), gold, 18f)
            setOnClickListener { addMoment() }
        }
        root.addView(add, LinearLayout.LayoutParams(-1, -2))
        root.addView(label("Categorias", 18f, Color.WHITE).apply { setPadding(2, 24, 0, 10) })
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        renderChips(chips)
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 18, 0, 0) }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshList()
    }

    private fun renderChips(chips: LinearLayout) {
        chips.removeAllViews()
        categories.forEach { category ->
            val chip = TextView(this).apply {
                text = category; textSize = 12f; gravity = Gravity.CENTER; setPadding(14, 9, 14, 9)
                setTextColor(if (selectedCategory == category) bg else Color.WHITE)
                background = rounded(if (selectedCategory == category) gold else panel, if (selectedCategory == category) gold else Color.rgb(65, 73, 85), 30f)
                setOnClickListener { selectedCategory = category; refreshList(); renderChips(chips) }
            }
            val lp = LinearLayout.LayoutParams(-2, -2); lp.rightMargin = 8; chips.addView(chip, lp)
        }
    }

    private fun refreshList() {
        list.removeAllViews(); valueViews.clear()
        val all = Moment.all(prefs).sortedBy { if (it.countdown) it.dateMillis else -it.dateMillis }
        val filtered = if (selectedCategory == "Todos") all else all.filter { it.category == selectedCategory }
        if (filtered.isEmpty()) {
            list.addView(label(if (all.isEmpty()) "Ainda não há momentos cadastrados.\nAdicione uma data especial para começar." else "Nenhuma data nesta categoria.", 15f, muted).apply { setPadding(8, 28, 8, 8) })
        }
        filtered.forEach { m ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(17, 15, 17, 15); background = rounded(panel, Color.rgb(65, 73, 85), 22f) }
            val categoryColor = when (m.category) { "Amor ❤️" -> Color.rgb(211, 133, 145); "Trabalho 💼" -> gold; "Metas 🎯" -> Color.rgb(130, 177, 164); "Viagens ✈️" -> Color.rgb(130, 165, 205); else -> muted }
            card.addView(label(m.category, 12f, categoryColor, true))
            card.addView(label("${m.emoji}  ${m.title}", 19f, Color.WHITE, true).apply { setPadding(0, 8, 0, 3) })
            val value = label(describe(m), 15f, gold).apply { setPadding(0, 5, 0, 2) }
            valueViews[m.id] = value; card.addView(value)
            card.addView(label(SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt","BR")).format(Date(m.dateMillis)) + "  •  " + if (m.countdown) "Contagem regressiva" else "Contagem crescente", 11f, muted).apply { setPadding(0, 4, 0, 0) })
            card.addView(label("Excluir", 12f, Color.rgb(225, 126, 130)).apply {
                setPadding(0, 12, 0, 0); setOnClickListener {
                    AlertDialog.Builder(this@MainActivity).setMessage("Excluir este momento?")
                        .setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ -> Moment.remove(prefs, m.id); refreshList(); updateWidget() }.show()
                }
            })
            val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = 13; list.addView(card, lp)
        }
    }

    private fun describe(m: Moment): String {
        val diff = if (m.countdown) m.dateMillis - System.currentTimeMillis() else System.currentTimeMillis() - m.dateMillis
        if (diff < 0) return "Data já passou"
        val totalSeconds = diff / 1000
        val days = totalSeconds / 86400
        val years = days / 365
        val restDays = days % 365
        val hours = (totalSeconds % 86400) / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return if (years > 0) "${years} ano(s) • ${restDays} dias • ${hours}h ${mins}m ${secs}s"
        else "${days} dias • ${hours}h ${mins}m ${secs}s"
    }

    private fun addMoment() {
        val name = EditText(this).apply { hint = "Nome da data"; setSingleLine(true) }
        val emoji = EditText(this).apply { hint = "Emoji (ex.: ❤️, 💼, 🎂)"; setText("📅"); setSingleLine(true) }
        val category = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, categories.drop(1)) }
        val check = CheckBox(this).apply { text = "Data futura (contagem regressiva)" }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0)
            addView(name); addView(emoji)
            addView(label("Categoria", 13f, Color.DKGRAY).apply { setPadding(0, 10, 0, 2) })
            addView(category); addView(check)
        }
        val dlg = AlertDialog.Builder(this).setTitle("Novo momento").setView(box)
            .setPositiveButton("Escolher data") { _, _ ->
                if (name.text.isBlank()) { Toast.makeText(this, "Digite um nome", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val now = Calendar.getInstance()
                DatePickerDialog(this, { _, y, mo, d ->
                    TimePickerDialog(this, { _, hh, mm ->
                        val c = Calendar.getInstance(); c.set(y, mo, d, hh, mm, 0); c.set(Calendar.MILLISECOND, 0)
                        Moment(title = name.text.toString().trim(), dateMillis = c.timeInMillis, emoji = emoji.text.toString().ifBlank { "📅" }, countdown = check.isChecked, category = category.selectedItem.toString()).save(prefs)
                        refreshList(); updateWidget()
                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
            }.setNegativeButton("Cancelar", null).create()
        dlg.show()
    }

    private fun updateWidget() {
        sendBroadcast(Intent(this, MomentWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, MomentWidget::class.java))))
    }
    override fun onResume() { super.onResume(); if (::list.isInitialized) refreshList(); timerHandler.removeCallbacks(secondTicker); timerHandler.post(secondTicker) }
    override fun onPause() { timerHandler.removeCallbacks(secondTicker); super.onPause() }
}
