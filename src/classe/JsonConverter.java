package src.classe;

import java.lang.reflect.Field;
import java.util.*;

public class JsonConverter {

    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        return objectToJsonString(obj);
    }

    private static String objectToJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }

        Class<?> clazz = obj.getClass();

        // Types simples
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }

        // Maps
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }

        // Collections
        if (obj instanceof Collection) {
            return collectionToJson((Collection<?>) obj);
        }

        // Objets complexes
        return objectToJsonObject(obj);
    }

    private static String objectToJsonObject(Object obj) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append("\"").append(field.getName()).append("\":");
                    json.append(objectToJsonString(value));
                    first = false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        json.append("}");
        return json.toString();
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            json.append(objectToJsonString(entry.getValue()));
            first = false;
        }

        json.append("}");
        return json.toString();
    }

    private static String collectionToJson(Collection<?> collection) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Object item : collection) {
            if (!first) {
                json.append(",");
            }
            json.append(objectToJsonString(item));
            first = false;
        }

        json.append("]");
        return json.toString();
    }

    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String createJsonResponse(String status, Object data) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"status\":\"").append(status).append("\",");
        json.append("\"data\":").append(objectToJsonString(data));
        json.append("}");
        return json.toString();
    }

    public static String createJsonError(String status, String message) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"status\":\"").append(status).append("\",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\"");
        json.append("}");
        return json.toString();
    }
}
