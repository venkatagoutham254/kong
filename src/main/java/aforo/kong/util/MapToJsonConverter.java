package aforo.kong.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return attribute == null ? null : objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Error converting map to JSON", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? new HashMap<>() : objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to map", e);
        }
    }
}
