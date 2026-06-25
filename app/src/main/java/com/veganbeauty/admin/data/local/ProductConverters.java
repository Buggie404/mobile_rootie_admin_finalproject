package com.veganbeauty.admin.data.local;

import androidx.room.TypeConverter;
import com.veganbeauty.admin.data.local.entities.KeyIngredient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductConverters {
    @TypeConverter
    public String fromStringList(List<String> value) {
        if (value == null) {
            return "[]";
        }
        return new JSONArray(value).toString();
    }

    @TypeConverter
    public List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (Exception e) {
            // Handle cases where the value is a simple comma-separated string instead of a JSON array
            String[] split = value.split(",");
            for (String item : split) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    @TypeConverter
    public String fromKeyIngredients(List<KeyIngredient> value) {
        if (value == null) {
            return "[]";
        }
        JSONArray array = new JSONArray();
        try {
            for (KeyIngredient item : value) {
                JSONObject obj = new JSONObject();
                obj.put("name", item.getName());
                obj.put("description", item.getDescription());
                array.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    @TypeConverter
    public List<KeyIngredient> toKeyIngredients(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<KeyIngredient> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                KeyIngredient ki = new KeyIngredient();
                ki.setName(obj.optString("name", ""));
                ki.setDescription(obj.optString("description", ""));
                list.add(ki);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
