package com.veganbeauty.admin.data.local;

import androidx.room.TypeConverter;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderConverters {
    @TypeConverter
    public String fromItemList(List<OrderItem> items) {
        if (items == null) {
            return "[]";
        }
        JSONArray array = new JSONArray();
        try {
            for (OrderItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("productId", item.getProductId());
                obj.put("productName", item.getProductName());
                obj.put("productImage", item.getProductImage());
                obj.put("quantity", item.getQuantity());
                obj.put("price", item.getPrice());
                array.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    @TypeConverter
    public List<OrderItem> toItemList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrderItem> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                OrderItem oi = new OrderItem();
                oi.setProductId(obj.getString("productId"));
                oi.setProductName(obj.getString("productName"));
                oi.setProductImage(obj.getString("productImage"));
                oi.setQuantity(obj.getInt("quantity"));
                oi.setPrice(obj.getLong("price"));
                list.add(oi);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
