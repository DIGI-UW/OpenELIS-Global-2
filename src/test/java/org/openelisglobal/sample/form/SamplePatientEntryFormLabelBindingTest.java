package org.openelisglobal.sample.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.fhir.springserialization.QuestionnaireDeserializer;
import org.openelisglobal.fhir.springserialization.QuestionnaireResponseDeserializer;
import org.openelisglobal.fhir.springserialization.QuestionnaireResponseSerializer;
import org.openelisglobal.fhir.springserialization.QuestionnaireSerializer;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;

/**
 * OGC-285 M5b — proves the {@code @RequestBody SamplePatientEntryForm} save
 * contract carries the new top-level {@code labelPersistRequest} block.
 *
 * <p>
 * The save endpoint binds the POST body via {@code @RequestBody} (Jackson), so
 * the only thing the HTTP layer needs to do for the M5b payload to reach the
 * hook is deserialize {@code labelPersistRequest} into the form. This test
 * locks that: a representative {@code JSON.stringify(orderFormValues)} body
 * (snake_case nested keys, plus unknown fields the real frontend sends)
 * deserializes into a populated {@link OrderLabelPersistRequest}, and a body
 * without the block leaves it null (so the hook's null-guard holds and existing
 * saves are untouched).
 */
public class SamplePatientEntryFormLabelBindingTest {

    private static ObjectMapper JSON;

    /**
     * Build the same Jackson mapper the save endpoint binds {@code @RequestBody}
     * with — {@code AppConfig.jacksonMessageConverter()} registers custom
     * Questionnaire(Response) (de)serializers; without them, introspecting the
     * form's FHIR {@code QuestionnaireResponse} field throws a conflicting-setter
     * error. Mirroring that config makes this a faithful binding-contract test, not
     * a vanilla-mapper approximation.
     */
    @BeforeClass
    public static void buildProductionEquivalentMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Questionnaire.class, new QuestionnaireSerializer());
        module.addDeserializer(Questionnaire.class, new QuestionnaireDeserializer());
        module.addSerializer(QuestionnaireResponse.class, new QuestionnaireResponseSerializer());
        module.addDeserializer(QuestionnaireResponse.class, new QuestionnaireResponseDeserializer());
        mapper.registerModule(module);
        JSON = mapper;
    }

    @Test
    public void deserializes_labelPersistRequest_fromSaveBody() throws Exception {
        // Mirrors the frontend's JSON.stringify(orderFormValues): the new
        // top-level labelPersistRequest with snake_case cell keys, alongside
        // unknown fields the form ignores via @JsonIgnoreProperties.
        String body = "{" + "\"warning\":false," + "\"someUnknownFrontendOnlyField\":\"ignored\","
                + "\"labelPersistRequest\":{" + "\"order_cells\":[{\"preset_id\":7,\"qty\":2}]," + "\"sample_rows\":["
                + "{\"sample_id_local\":\"0\",\"cells\":[{\"preset_id\":9,\"qty\":1}]},"
                + "{\"sample_id_local\":\"1\",\"cells\":[{\"preset_id\":9,\"qty\":3}]}" + "]}}";

        SamplePatientEntryForm form = JSON.readValue(body, SamplePatientEntryForm.class);

        OrderLabelPersistRequest payload = form.getLabelPersistRequest();
        assertNotNull("labelPersistRequest must bind from the save body", payload);

        assertEquals(1, payload.getOrderCells().size());
        assertEquals(Integer.valueOf(7), payload.getOrderCells().get(0).getPresetId());
        assertEquals(Integer.valueOf(2), payload.getOrderCells().get(0).getQty());

        assertEquals(2, payload.getSampleRows().size());
        assertEquals("0", payload.getSampleRows().get(0).getSampleIdLocal());
        assertEquals("1", payload.getSampleRows().get(1).getSampleIdLocal());
        assertEquals(Integer.valueOf(9), payload.getSampleRows().get(1).getCells().get(0).getPresetId());
        assertEquals(Integer.valueOf(3), payload.getSampleRows().get(1).getCells().get(0).getQty());
    }

    @Test
    public void labelPersistRequest_isNull_whenAbsentFromBody() throws Exception {
        SamplePatientEntryForm form = JSON.readValue("{\"warning\":false}", SamplePatientEntryForm.class);
        assertNull("a save body without labelPersistRequest leaves the field null (hook stays guarded)",
                form.getLabelPersistRequest());
    }
}
