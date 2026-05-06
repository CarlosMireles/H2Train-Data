package com.h2traindata.infrastructure.datalake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonEventSerializer {

    private final ObjectMapper objectMapper;

    public JsonEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(BronzeEventRecord eventRecord) {
        try {
            return objectMapper.writeValueAsString(eventRecord);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize bronze event record", exception);
        }
    }
}
