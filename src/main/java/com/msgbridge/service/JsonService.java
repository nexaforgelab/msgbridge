package com.msgbridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgbridge.core.BusinessException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JsonService {
    private final ObjectMapper objectMapper;

    public JsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("JSON serialization failed: " + e.getOriginalMessage());
        }
    }

    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("JSON parsing failed: " + e.getOriginalMessage());
        }
    }

    public Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("JSON object parsing failed: " + e.getOriginalMessage());
        }
    }

    public List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            if (json.trim().startsWith("[")) {
                return objectMapper.readValue(json, new TypeReference<List<String>>() {
                });
            }
            Map<String, Object> map = readMap(json);
            Object channels = map.get("channels");
            return objectMapper.convertValue(channels, new TypeReference<List<String>>() {
            });
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw BusinessException.badRequest("channel list parsing failed");
        }
    }

    public <T> T convert(Object value, Class<T> type) {
        return objectMapper.convertValue(value, type);
    }
}
