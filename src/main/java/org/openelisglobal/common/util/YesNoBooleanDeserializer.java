package org.openelisglobal.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Locale;

/**
 * Reads a boolean that a form also serializes as "Y"/"N", so a row read from an
 * endpoint can be posted back to it unchanged. JSON booleans, "true"/"false"
 * and 1/0 are accepted as well; anything else is rejected.
 */
public class YesNoBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_NULL) {
            return Boolean.FALSE;
        }
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue() != 0;
        }
        String text = parser.getValueAsString("").trim().toUpperCase(Locale.ROOT);
        switch (text) {
        case "Y":
        case "YES":
        case "TRUE":
        case "1":
            return Boolean.TRUE;
        case "N":
        case "NO":
        case "FALSE":
        case "0":
        case "":
            return Boolean.FALSE;
        default:
            return (Boolean) context.handleWeirdStringValue(Boolean.class, text,
                    "only true/false or Y/N are recognized");
        }
    }

    @Override
    public Boolean getNullValue(DeserializationContext context) {
        return Boolean.FALSE;
    }
}
