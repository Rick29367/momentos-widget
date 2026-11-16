package br.com.momentos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MomentWidget : AppWidgetProvider() {
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {
        val moments=Moment.all(context.getSharedPreferences("momentos",Context.MODE_PRIVATE))
        for(id in ids) {
            val views=RemoteViews(context.packageName,R.layout.moment_widget)
            val m=moments.firstOrNull()
            if(m==null) { views.setTextViewText(R.id.widget_title,"✨ Momentos"); views.setTextViewText(R.id.widget_value,"Adicione sua primeira data"); views.setTextViewText(R.id.widget_detail,"Toque para abrir o aplicativo") }
            else {
                views.setTextViewText(R.id.widget_title,"${m.emoji} ${m.title}")
                val delta=if(m.countdown) m.dateMillis-System.currentTimeMillis() else System.currentTimeMillis()-m.dateMillis
                val safe=delta.coerceAtLeast(0)
                val days=safe/86400000; val hours=(safe/3600000)%24; val mins=(safe/60000)%60
                views.setTextViewText(R.id.widget_value,if(m.countdown) "Faltam ${days}d ${hours}h ${mins}min" else "${days} dias juntos")
                views.setTextViewText(R.id.widget_detail,if(m.countdown) "Até a data marcada" else "Desde sua data especial")
            }
            val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root,open)
            manager.updateAppWidget(id,views)
        }
    }
}
