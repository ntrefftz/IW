package es.ucm.fdi.iw.model.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ucm.fdi.iw.model.UnoState;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UnoStateConverter implements AttributeConverter<UnoState, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(UnoState attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar UnoState a JSON", e);
        }
    }

    @Override
    public UnoState convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return MAPPER.readValue(dbData, UnoState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo deserializar UnoState desde JSON", e);
        }
    }
}
