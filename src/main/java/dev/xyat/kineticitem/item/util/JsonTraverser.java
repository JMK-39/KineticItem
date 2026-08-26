package dev.xyat.kineticitem.item.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

public class JsonTraverser {
    public static void replaceIds(JsonElement element, Map<String, String> map) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) return;
        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement child = arr.get(i);
                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    String val = child.getAsString();
                    if (map.containsKey(val)) arr.set(i, new JsonPrimitive(map.get(val)));
                } else replaceIds(child, map);
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonElement child = entry.getValue();
                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    String val = child.getAsString();
                    if (map.containsKey(val)) obj.addProperty(entry.getKey(), map.get(val));
                } else replaceIds(child, map);
            }
        }
    }
}