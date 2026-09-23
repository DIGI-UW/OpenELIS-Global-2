package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.service.ReportSourceConfigCodec;

public class ReportSourceConfigTest {
    private String config(String id, String source, String label) {
        return """
                {"id":"%s","version":1,"label":"%s","source":"%s",
                "dateAnchor":"COLLECTION_DATE","layouts":["SPREADSHEET","RESULT_LIST"],
                "attributes":["accessionNumber","collectionDate"],
                "catalogs":["TEST_COMPONENTS","OBSERVATION_TYPES","QUESTIONS"],
                "filters":["LAB_SECTIONS","TESTS","RESULT_STATUSES"],
                "defaultColumns":{"SPREADSHEET":["accessionNumber","collectionDate"],
                "RESULT_LIST":["accessionNumber"]}}
                """.formatted(id, label, source);
    }

    private ReportSourceConfig parse(String json) throws Exception {
        return new ReportSourceConfigCodec().read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                Set.of("SAMPLE_TESTING"));
    }

    @Test
    public void anotherDefinitionCanReuseTheSameSupportedSource() throws Exception {
        ReportSourceConfig routine = parse(config("ROUTINE", "SAMPLE_TESTING", "Routine report"));
        ReportSourceConfig monthly = parse(config("MONTHLY", "SAMPLE_TESTING", "Monthly report"));
        assertEquals(routine.source(), monthly.source());
        assertNotEquals(routine.id(), monthly.id());
        assertEquals("Monthly report", monthly.label());
        assertEquals(2, monthly.layouts().size());
    }

    @Test
    public void invalidSourcesAndUnknownConfigurationKeysAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse(config("X", "ARBITRARY_SQL", "Bad source")));
        assertThrows(IllegalArgumentException.class,
                () -> parse(config("X", "SAMPLE_TESTING", "Typo").replace("\"dateAnchor\"", "\"dateAncor\"")));
    }

    @Test
    public void defaultsAndLayoutsCannotContradictTheDefinition() {
        assertThrows(IllegalArgumentException.class, () -> parse(config("X", "SAMPLE_TESTING", "Bad layout")
                .replace("\"SPREADSHEET\",\"RESULT_LIST\"", "\"SPREADSHEET\"")));
        assertThrows(IllegalArgumentException.class, () -> parse(config("X", "SAMPLE_TESTING", "Bad default")
                .replace("\"RESULT_LIST\":[\"accessionNumber\"]", "\"RESULT_LIST\":[\"unknownField\"]")));
    }

    @Test
    public void parsedDefinitionDoesNotAllowMutatingCapturedChoices() throws Exception {
        ReportSourceConfig definition = parse(config("X", "SAMPLE_TESTING", "Report"));
        assertThrows(UnsupportedOperationException.class, () -> definition.attributes().add("another"));
        assertThrows(UnsupportedOperationException.class, () -> definition.defaultColumns().get("SPREADSHEET").clear());
    }

    @Test
    public void dynamicDefaultGroupsAreExplicitAndMustBeAvailable() throws Exception {
        String json = config("X", "SAMPLE_TESTING", "Report").replace("\"RESULT_LIST\":[\"accessionNumber\"]",
                "\"RESULT_LIST\":[\"accessionNumber\",\"catalog:TEST_COMPONENTS\"]");
        assertEquals(List.of("accessionNumber", "catalog:TEST_COMPONENTS"),
                parse(json).defaultColumns().get("RESULT_LIST"));
        assertThrows(IllegalArgumentException.class,
                () -> parse(json.replace("catalog:TEST_COMPONENTS", "catalog:unknown")));
    }

    @Test
    public void duplicateDefaultsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(config("X", "SAMPLE_TESTING", "Report").replace("\"RESULT_LIST\":[\"accessionNumber\"]",
                        "\"RESULT_LIST\":[\"accessionNumber\",\"accessionNumber\"]")));
    }
}
