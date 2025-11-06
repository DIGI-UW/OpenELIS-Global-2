package org.openelisglobal.fhir.springserialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.spring.util.SpringContext;

public class QuestionnaireResponseSerializer extends StdSerializer<QuestionnaireResponse> {

    public QuestionnaireResponseSerializer() {
        this(null);
    }

    public QuestionnaireResponseSerializer(Class<QuestionnaireResponse> t) {
        super(t);
    }

    @Override
    public void serialize(QuestionnaireResponse value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value == null) {
            return;
        }
        JsonNode node = new ObjectMapper().readTree(
                SpringContext.getBean(FhirUtil.class).getFhirParser().encodeResourceToString(value).getBytes());
        jgen.writeTree(node);
    }
}
