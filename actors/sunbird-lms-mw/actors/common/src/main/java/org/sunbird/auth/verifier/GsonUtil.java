package org.sunbird.auth.verifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.util.Map;

public class GsonUtil {

    private static Gson sGson;

    public static Gson getGson() {
        if (sGson == null) {
            sGson = new GsonBuilder().create();
        }
        return sGson;
    }

    public static <C> C fromJson(String json, Class<C> classOfC) {
        return getGson().fromJson(json, classOfC);
    }

    public static <T> T fromJson(String json, Type type) {
        return (T) getGson().fromJson(json, type);
    }

    public static <T> T fromJson(String json, Class<T> type, String exceptionMessage) {
            return getGson().fromJson(json, type);
    }

    public static <C> C fromMap(Map map, Class<C> classOfC) {
        JsonElement jsonElement = getGson().toJsonTree(map);
        return getGson().fromJson(jsonElement, classOfC);
    }

    public static String toJson(Object json) {
        return getGson().toJson(json);
    }

}
