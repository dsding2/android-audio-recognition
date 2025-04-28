package app.danielding.voiceactivation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object TuningStorage {
    private val Context.dataStore by preferencesDataStore(name = "tuning_data_store")
    private val TUNING_MAP_KEY = stringPreferencesKey("tuning_map_key")

    // Save a Map<String, Uri> to the DataStore
    fun saveData(context: Context, data: Map<String, Double>) {
        // Serialize the map to a JSON string
        val jsonString = mapToJson(data)

        // Save the JSON string in DataStore
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[TUNING_MAP_KEY] = jsonString
            }
        }
    }

    fun putData(context: Context, key: String, value: Double) {
        val data = getData(context).toMutableMap()
        data[key] = value
        saveData(context, data)
    }

    fun getData(context: Context): Map<String, Double> {
        var data: Map<String, Double> = emptyMap()
        runBlocking {
            val preferences = context.dataStore.data.first()
            val jsonString = preferences[TUNING_MAP_KEY]
            if (jsonString != null) {
                data = jsonToMap(jsonString)
            }
        }
        return data
    }

    fun getValue(context: Context, key: String): Double {
        return getData(context)[key] ?: Globals.DEFAULT_TUNING
    }

    fun clear(context: Context) {
        saveData(context, mapOf())
    }

    private fun mapToJson(map: Map<String, Double>): String {
        val jsonEntries = map.map {
            "${it.key}||${it.value}"
        }.joinToString(",")

        return jsonEntries
    }

    // Convert JSON string back to Map<String, Uri>
    private fun jsonToMap(json: String): Map<String, Double> {
        val entries = json.split(",")
        return entries.mapNotNull { entry ->
            val parts = entry.split("||")
            if (parts.size == 2) {
                val (key, value) = parts
                if (value.toDoubleOrNull() == null) {
                    null
                } else {
                    key to value.toDouble()
                }
            } else {
                null  // skip malformed entry
            }
        }.toMap()
    }
}
