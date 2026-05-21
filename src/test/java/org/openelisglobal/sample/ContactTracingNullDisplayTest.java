package org.openelisglobal.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-513: Patient Status Report PDF displays literal "null" for empty Contact
 * Tracing fields.
 *
 * These tests assert the CORRECT behavior: when a sample has no contact tracing
 * data, the rendered note must not contain literal "null". They will FAIL until
 * the bug is fixed, then PASS after the fix.
 */
public class ContactTracingNullDisplayTest extends BaseWebContextSensitiveTest {

    private static final String SAMPLE_ID = "1";

    @Autowired
    private SampleService sampleService;

    @Before
    public void setUp() throws Exception {
        // Load sample fixture — sample id=1 has NO rows in sample_additional_fields
        executeDataSetWithStateManagement("testdata/samplehuman.xml");
    }

    @Test
    public void getContactTracingFieldValue_shouldReturnEmptyString_whenNoContactTracingData() {
        SampleAdditionalField field = sampleService.getSampleAdditionalFieldForSample(SAMPLE_ID,
                AdditionalFieldName.CONTACT_TRACING_INDEX_NAME);

        assertNotNull("Service should return a SampleAdditionalField, not null", field);

        // Expected: empty string (or null handled upstream), not raw null that
        // becomes literal "null" in string concatenation
        assertNotNull("fieldValue should not be null — it should default to empty string", field.getFieldValue());
        assertEquals("fieldValue should be empty when no contact tracing data exists", "", field.getFieldValue());
    }

    @Test
    public void getContactTracingRecordNumber_shouldReturnEmptyString_whenNoContactTracingData() {
        SampleAdditionalField field = sampleService.getSampleAdditionalFieldForSample(SAMPLE_ID,
                AdditionalFieldName.CONTACT_TRACING_INDEX_RECORD_NUMBER);

        assertNotNull(field);
        assertNotNull("fieldValue should not be null — it should default to empty string", field.getFieldValue());
        assertEquals("fieldValue should be empty when no contact tracing data exists", "", field.getFieldValue());
    }

    @Test
    public void renderedContactTracingNote_shouldNotContainLiteralNull() {
        SampleAdditionalField nameField = sampleService.getSampleAdditionalFieldForSample(SAMPLE_ID,
                AdditionalFieldName.CONTACT_TRACING_INDEX_NAME);
        SampleAdditionalField recordField = sampleService.getSampleAdditionalFieldForSample(SAMPLE_ID,
                AdditionalFieldName.CONTACT_TRACING_INDEX_RECORD_NUMBER);

        // Simulate PatientReport.java setting the data bean
        ClinicalPatientData data = new ClinicalPatientData();
        data.setContactTracingIndexName(nameField.getFieldValue());
        data.setContactTracingIndexRecordNumber(recordField.getFieldValue());

        // Simulate the JRXML concatenation (PatientReportCDI_vreduit.jrxml:799)
        String noteLabel = "Note";
        String indexNameLabel = "Contact Tracing Index Name";
        String indexRecordLabel = "Contact Tracing Index Record Number";

        String rendered = noteLabel + ": " + indexNameLabel + ": " + data.getContactTracingIndexName() + " "
                + indexRecordLabel + ": " + data.getContactTracingIndexRecordNumber();

        // The rendered note must NOT contain literal "null"
        assertFalse("Rendered note should not contain literal 'null' — got: " + rendered, rendered.contains("null"));
    }
}
