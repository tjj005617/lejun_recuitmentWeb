package com.interview.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * 用于清洗 AI 响应中的 JSON 并解析
 */
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 清洗 AI 响应中的 JSON
     * 移除 markdown 代码块标记
     */
    public static String cleanJsonResponse(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
    }

    /**
     * 从响应中提取 JSON 对象（{...}）
     */
    public static String extractJsonObject(String raw) {
        String json = cleanJsonResponse(raw);
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return json.substring(start, end + 1);
        }
        return json;
    }

    /**
     * 从响应中提取 JSON 数组（[...]）
     */
    public static String extractJsonArray(String raw) {
        String json = cleanJsonResponse(raw);
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return json.substring(start, end + 1);
        }
        return json;
    }

    /**
     * 解析 JSON 为对象
     */
    public static <T> T parseJson(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }

    /**
     * 解析 JSON 为 List<Map>
     */
    public static List<Map<String, Object>> parseJsonList(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
    }
}
