package org.openelisglobal.fhir.springserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.spring.util.SpringContext;

public class QuestionnaireDeserializer extends StdDeserializer<Questionnaire> {

    public QuestionnaireDeserializer() {
        this(null);
    }

    public QuestionnaireDeserializer(Class<Questionnaire> vc) {
        super(vc);
    }

    @Override
    public Questionnaire deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        return (Questionnaire) SpringContext.getBean(FhirUtil.class).getFhirParser().parseResource(node.toString());
    }
}
