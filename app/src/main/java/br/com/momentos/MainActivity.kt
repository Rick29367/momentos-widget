package br.com.momentos

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("momentos", Context.MODE_PRIVATE) }
    private lateinit var list: LinearLayout
    private val valueViews = mutableMapOf<String, TextView>()
    private val timerHandler = Handler(Looper.getMainLooper())
    private val secondTicker = object : Runnable {
        override fun run() {
            valueViews.forEach { (id, view) ->
                Moment.all(prefs).firstOrNull { it.id == id }?.let { view.text = describe(it) }
            }
            timerHandler.postDelayed(this, 1000)
        }
    }
    private val pink = Color.rgb(210,105,145)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    private fun render() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,28,24,24); setBackgroundColor(Color.rgb(247,244,247)) }
        root.addView(TextView(this).apply { text="✨ MOMENTOS"; textSize=27f; setTextColor(Color.rgb(45,43,52)); setTypeface(null,1) })
        root.addView(TextView(this).apply { text="Suas datas importantes, sempre por perto."; textSize=14f; setTextColor(Color.DKGRAY); setPadding(0,4,0,20) })
        val add=Button(this).apply { text="+ ADICIONAR DATA"; setOnClickListener { addMoment() } }; root.addView(add)
        val scroll=ScrollView(this); list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,16,0,0) }; scroll.addView(list)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root); refreshList()
    }
    private fun refreshList() {
        list.removeAllViews()
        valueViews.clear()
        val moments=Moment.all(prefs).sortedBy { if(it.countdown) it.dateMillis else -it.dateMillis }
        if(moments.isEmpty()) list.addView(TextView(this).apply { text="Nenhuma data cadastrada ainda.\nAdicione seu relacionamento, emprego, aniversários e conquistas."; textSize=16f; setTextColor(Color.DKGRAY); setPadding(8,30,8,8) })
        moments.forEach { m ->
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,14,16,14); setBackgroundColor(Color.WHITE) }
            card.addView(TextView(this).apply { text="${m.emoji}  ${m.title}"; textSize=18f; setTextColor(Color.rgb(45,43,52)); setTypeface(null,1) })
            val value = TextView(this).apply { text=describe(m); textSize=15f; setTextColor(pink); setPadding(0,8,0,0) }
            valueViews[m.id] = value
            card.addView(value)
            card.addView(TextView(this).apply { text=SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",Locale("pt","BR")).format(Date(m.dateMillis))+"  •  "+if(m.countdown) "Contagem regressiva" else "Contagem crescente"; textSize=11f; setTextColor(Color.GRAY); setPadding(0,5,0,0) })
            val del=TextView(this).apply { text="Excluir"; textSize=12f; setTextColor(Color.RED); setPadding(0,10,0,0); setOnClickListener { Moment.remove(prefs,m.id); refreshList(); updateWidget() } }
            card.addView(del); val lp=LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=12; list.addView(card,lp)
        }
    }
    private fun describe(m:Moment):String {
        val diff=if(m.countdown) m.dateMillis-System.currentTimeMillis() else System.currentTimeMillis()-m.dateMillis
        if(diff<0) return "Data já passou"
        val sec=diff/1000; val days=sec/86400; val h=(sec%86400)/3600; val min=(sec%3600)/60; val s=sec%60
        return if(days>=365) "${days/365} anos • ${days%365} dias • ${h}h ${min}m ${s}s" else "${days} dias • ${h}h ${min}m ${s}s"
    }
    private fun addMoment() {
        val name=EditText(this).apply { hint="Ex.: Meu amor, emprego, aniversário" }
        val emoji=EditText(this).apply { hint="Emoji (ex.: ❤️, 💼, 🎂)"; setText("📅") }
        val check=CheckBox(this).apply { text="É uma data futura (contagem regressiva)" }
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,8,24,0); addView(name); addView(emoji); addView(check) }
        var chosen=Calendar.getInstance()
        val dlg=AlertDialog.Builder(this).setTitle("Nova data importante").setView(box)
            .setPositiveButton("Escolher data") { _,_->
                if(name.text.isBlank()) { Toast.makeText(this,"Digite um nome para a data",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                DatePickerDialog(this,{ _,y,mo,d ->
                    val c=Calendar.getInstance(); c.set(y,mo,d,chosen.get(Calendar.HOUR_OF_DAY),chosen.get(Calendar.MINUTE),0); c.set(Calendar.MILLISECOND,0)
                    TimePickerDialog(this,{ _,hh,mm -> c.set(Calendar.HOUR_OF_DAY,hh); c.set(Calendar.MINUTE,mm)
                        Moment(title=name.text.toString().trim(),dateMillis=c.timeInMillis,emoji=emoji.text.toString().ifBlank{"📅"},countdown=check.isChecked).save(prefs)
                        refreshList(); updateWidget()
                    },chosen.get(Calendar.HOUR_OF_DAY),chosen.get(Calendar.MINUTE),true).show()
                },chosen.get(Calendar.YEAR),chosen.get(Calendar.MONTH),chosen.get(Calendar.DAY_OF_MONTH)).show()
            }.setNegativeButton("Cancelar",null).create()
        dlg.show()
    }
    private fun updateWidget() { sendBroadcast(Intent(this,MomentWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this,MomentWidget::class.java)))) }
    override fun onResume(){ super.onResume(); if(::list.isInitialized) refreshList(); timerHandler.removeCallbacks(secondTicker); timerHandler.post(secondTicker) }
    override fun onPause(){ timerHandler.removeCallbacks(secondTicker); super.onPause() }
}
