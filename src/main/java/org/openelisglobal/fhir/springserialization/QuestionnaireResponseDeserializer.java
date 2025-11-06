package org.openelisglobal.fhir.springserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.spring.util.SpringContext;

public class QuestionnaireResponseDeserializer extends StdDeserializer<QuestionnaireResponse> {

    public QuestionnaireResponseDeserializer() {
        this(null);
    }

    public QuestionnaireResponseDeserializer(Class<QuestionnaireResponse> vc) {
        super(vc);
    }

    @Override
    public QuestionnaireResponse deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        return (QuestionnaireResponse) SpringContext.getBean(FhirUtil.class).getFhirParser()
                .parseResource(node.toString());
    }
}
