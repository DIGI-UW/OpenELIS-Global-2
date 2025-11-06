package org.openelisglobal.fhir.springserialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.spring.util.SpringContext;

public class QuestionnaireSerializer extends StdSerializer<Questionnaire> {

    public QuestionnaireSerializer() {
        this(null);
    }

    public QuestionnaireSerializer(Class<Questionnaire> t) {
        super(t);
    }

    @Override
    public void serialize(Questionnaire value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (value == null) {
            return;
        }
        JsonNode node = new ObjectMapper().readTree(
                SpringContext.getBean(FhirUtil.class).getFhirParser().encodeResourceToString(value).getBytes());
        jgen.writeTree(node);
    }
}
