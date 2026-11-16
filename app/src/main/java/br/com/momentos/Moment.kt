package br.com.momentos

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Moment(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dateMillis: Long,
    val emoji: String = "📅",
    val countdown: Boolean = false
) {
    fun save(prefs: android.content.SharedPreferences) {
        val all = JSONArray(prefs.getString("moments", "[]"))
        val out = JSONArray()
        for (i in 0 until all.length()) if (all.getJSONObject(i).optString("id") != id) out.put(all.getJSONObject(i))
        out.put(JSONObject().put("id",id).put("title",title).put("date",dateMillis).put("emoji",emoji).put("countdown",countdown))
        prefs.edit().putString("moments",out.toString()).apply()
    }
    companion object {
        fun all(prefs: android.content.SharedPreferences): List<Moment> {
            val a=JSONArray(prefs.getString("moments","[]"))
            return (0 until a.length()).map { val o=a.getJSONObject(it); Moment(o.optString("id"),o.optString("title"),o.optLong("date"),o.optString("emoji","📅"),o.optBoolean("countdown")) }
        }
        fun remove(prefs: android.content.SharedPreferences,id:String) {
            val a=JSONArray(prefs.getString("moments","[]")); val b=JSONArray()
            for(i in 0 until a.length()) if(a.getJSONObject(i).optString("id")!=id) b.put(a.getJSONObject(i))
            prefs.edit().putString("moments",b.toString()).apply()
        }
    }
}
