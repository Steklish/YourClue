package com.example.tstproj

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type

class LocalStorageHandler(private val context: Context) : JsonStorage {

    private val gson = Gson()

    /**
     * Serializes the given data object into a JSON string and saves it to a file in the app's internal storage.
     * @param fileName The name of the file to save the data to.
     * @param data The object to be saved.
     */
    override fun <T> writeJsonToFile(fileName: String, data: T) {
        val file = File(context.filesDir, fileName)
        try {
            FileWriter(file).use {
                gson.toJson(data, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reads a JSON file from the app's internal storage and deserializes it into an object of the specified type.
     * @param fileName The name of the file to read the data from.
     * @param type The type of the object to deserialize the JSON into.
     * @return The deserialized object, or null if the file does not exist or an error occurs.
     */
    override fun <T> readJsonFromFile(fileName: String, type: Type): T? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            return null
        }
        return try {
            FileReader(file).use {
                gson.fromJson(it, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
