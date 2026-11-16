package br.com.momentos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MomentWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("momentos", Context.MODE_PRIVATE)
        val all = Moment.all(prefs)
        for (id in ids) {
            val selectedId = prefs.getString("widget_event_$id", null)
            val moment = all.firstOrNull { it.id == selectedId } ?: all.firstOrNull()
            val views = RemoteViews(context.packageName, R.layout.moment_widget)
            if (moment == null) {
                views.setTextViewText(R.id.widget_category, "MOMENTOS")
                views.setTextViewText(R.id.widget_title, "Seu próximo momento começa aqui")
                views.setTextViewText(R.id.widget_value, "Adicione uma data no aplicativo")
                views.setTextViewText(R.id.widget_detail, "Toque para escolher um evento")
            } else {
                val delta = if (moment.countdown) moment.dateMillis - System.currentTimeMillis() else System.currentTimeMillis() - moment.dateMillis
                val safe = delta.coerceAtLeast(0L)
                val totalSeconds = safe / 1000
                val days = totalSeconds / 86400
                val hours = (totalSeconds % 86400) / 3600
                val minutes = (totalSeconds % 3600) / 60
                val years = days / 365
                val restDays = days % 365
                val value = if (moment.countdown) "Faltam $days dias • ${hours}h ${minutes}min"
                    else if (years > 0) "$years ano(s) • $restDays dias"
                    else "$days dias • ${hours}h ${minutes}min"
                views.setTextViewText(R.id.widget_category, moment.category.uppercase(Locale("pt", "BR")))
                views.setTextViewText(R.id.widget_title, "${moment.emoji}  ${moment.title}")
                views.setTextViewText(R.id.widget_value, value)
                val date = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("pt", "BR")).format(Date(moment.dateMillis))
                views.setTextViewText(R.id.widget_detail, (if (moment.countdown) "ATÉ " else "DESDE ") + date)
            }
            val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val configure = PendingIntent.getActivity(context, id + 100000, Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_change, configure)
            manager.updateAppWidget(id, views)
        }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences("momentos", Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach { editor.remove("widget_event_$it") }
        editor.apply()
        super.onDeleted(context, appWidgetIds)
    }
}
