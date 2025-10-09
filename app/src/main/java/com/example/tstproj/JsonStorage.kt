package com.example.tstproj

import java.lang.reflect.Type

interface JsonStorage {
    fun <T> writeJsonToFile(fileName: String, data: T)
    fun <T> readJsonFromFile(fileName: String, type: Type): T?
}
