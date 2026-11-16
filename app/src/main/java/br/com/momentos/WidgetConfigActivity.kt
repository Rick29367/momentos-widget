package br.com.momentos

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

class WidgetConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val prefs = getSharedPreferences("momentos", Context.MODE_PRIVATE)
        val moments = Moment.all(prefs)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 28, 28, 24) }
        root.addView(TextView(this).apply { text = "Escolha o momento deste widget"; textSize = 21f; setTextColor(0xFF202735.toInt()); setPadding(0, 0, 0, 14) })
        if (moments.isEmpty()) {
            root.addView(TextView(this).apply { text = "Você ainda não cadastrou nenhuma data. Abra o MOMENTOS, adicione um evento e tente novamente."; textSize = 15f })
            setContentView(root)
            return
        }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@WidgetConfigActivity, android.R.layout.simple_spinner_dropdown_item, moments.map { "${it.emoji}  ${it.title}  •  ${it.category}" })
        }
        val previous = prefs.getString("widget_event_$widgetId", null)
        val selected = moments.indexOfFirst { it.id == previous }
        if (selected >= 0) spinner.setSelection(selected)
        root.addView(spinner)
        root.addView(Button(this).apply {
            text = "Salvar widget"
            setOnClickListener {
                prefs.edit().putString("widget_event_$widgetId", moments[spinner.selectedItemPosition].id).apply()
                val update = Intent(this@WidgetConfigActivity, MomentWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
                sendBroadcast(update)
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            }
        })
        setContentView(root)
    }
}
