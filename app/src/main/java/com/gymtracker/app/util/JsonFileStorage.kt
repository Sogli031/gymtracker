package com.gymtracker.app.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Zajednički utility za atomsko čuvanje i učitavanje JSON fajlova.
 * Koristi temp fajl + rename pattern za sprečavanje korupcije podataka.
 */
object JsonFileStorage {

    private const val TAG = "JsonFileStorage"

    /**
     * Atomsko čuvanje podataka u JSON fajl.
     * Piše u temp fajl pa rename-uje, sa fallback-om na copy+delete.
     */
    suspend fun saveToFile(file: File, data: String) {
        withContext(Dispatchers.IO) {
            val tempFile = File(file.parent, "${file.name}.tmp")
            tempFile.writeText(data)
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
        }
    }

    /**
     * Učitavanje i parsiranje JSON fajla u objekat zadatog tipa.
     * Vraća null ako fajl ne postoji ili je neispravan.
     */
    inline fun <reified T> loadFromFile(file: File, gson: Gson): T? {
        if (!file.exists()) return null
        return try {
            val raw = file.readText()
            if (raw.isBlank()) return null
            gson.fromJson(raw, T::class.java)
        } catch (e: Exception) {
            Log.e("JsonFileStorage", "Greška pri čitanju ${file.name}: ${e.message}", e)
            null
        }
    }

    /**
     * Učitavanje i parsiranje JSON fajla u listu objekata zadatog tipa.
     * Vraća praznu listu ako fajl ne postoji ili je neispravan.
     */
    inline fun <reified T> loadListFromFile(file: File, gson: Gson): List<T> {
        if (!file.exists()) return emptyList()
        return try {
            val raw = file.readText()
            if (raw.isBlank()) return emptyList()
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson<List<T>>(raw, type).orEmpty()
        } catch (e: Exception) {
            Log.e("JsonFileStorage", "Greška pri čitanju ${file.name}: ${e.message}", e)
            emptyList()
        }
    }
}
