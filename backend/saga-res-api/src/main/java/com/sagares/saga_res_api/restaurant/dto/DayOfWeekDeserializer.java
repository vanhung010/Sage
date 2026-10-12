package com.sagares.saga_res_api.restaurant.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.DayOfWeek;

// Keep the API enum textual: Jackson otherwise accepts numeric enum ordinals.
public class DayOfWeekDeserializer extends ValueDeserializer<DayOfWeek> {

    @Override
    public DayOfWeek deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (DayOfWeek) context.handleUnexpectedToken(DayOfWeek.class, parser);
        }
        String value = parser.getString();
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return (DayOfWeek) context.handleWeirdStringValue(DayOfWeek.class, value, "Ngày trong tuần không hợp lệ");
        }
    }
}
