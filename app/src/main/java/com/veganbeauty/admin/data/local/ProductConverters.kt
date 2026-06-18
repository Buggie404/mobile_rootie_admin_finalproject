package com.veganbeauty.admin.data.local

import androidx.room.TypeConverter
import com.veganbeauty.admin.data.local.entities.KeyIngredient
import org.json.JSONArray
import org.json.JSONObject

class ProductConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.let { JSONArray(it).toString() } ?: "[]"
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // Handle cases where the value is a simple comma-separated string instead of a JSON array
            return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        return list
    }

    @TypeConverter
    fun fromKeyIngredients(value: List<KeyIngredient>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        for (item in value) {
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("description", item.description)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toKeyIngredients(value: String?): List<KeyIngredient> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<KeyIngredient>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    KeyIngredient(
                        name = obj.optString("name", ""),
                        description = obj.optString("description", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
