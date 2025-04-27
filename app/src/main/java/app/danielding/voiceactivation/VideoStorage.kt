package app.danielding.voiceactivation

import android.content.Context
import android.net.Uri

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

object VideoStorage {
    private val Context.dataStore by preferencesDataStore(name = "video_data_store")
    private val VIDEO_MAP_KEY = stringPreferencesKey("video_map_key")

    // Save a Map<String, Uri> to the DataStore
    fun saveData(context: Context, data: Map<String, Uri>) {
        // Serialize the map to a JSON string
        val jsonString = mapToJson(data)

        // Save the JSON string in DataStore
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[VIDEO_MAP_KEY] = jsonString
            }
        }
    }

    fun putData(context: Context, key: String, video: Uri) {
        val data = getData(context).toMutableMap()
        data[key] = video
        saveData(context, data)
    }

    // Retrieve the Map<String, Uri> from the DataStore
    fun getData(context: Context): Map<String, Uri> {
        var data: Map<String, Uri> = emptyMap()

        // Retrieve the JSON string from DataStore and deserialize it into a Map
        runBlocking {
            val preferences = context.dataStore.data.first()
            val jsonString = preferences[VIDEO_MAP_KEY]

            // If the JSON string is not null, deserialize it
            if (jsonString != null) {
                data = jsonToMap(jsonString)
            }
        }
        return data
    }

    private fun mapToJson(map: Map<String, Uri>): String {
        val jsonEntries = map.map {
            // Escape the key and Uri values properly for JSON
            "\"${it.key}\":\"${it.value.toString()}\""
        }.joinToString(",")

        return "{ $jsonEntries }"
    }

    // Convert JSON string back to Map<String, Uri>
    private fun jsonToMap(json: String): Map<String, Uri> {
        // Strip the outer curly braces and split into key-value pairs
        val entries = json.removeSurrounding("{", "}").split(",")

        return entries.associate { entry ->
            val (key, value) = entry.split(":").map { it.trim().removeSurrounding("\"") }
            key to value.toUri()
        }
    }
}
