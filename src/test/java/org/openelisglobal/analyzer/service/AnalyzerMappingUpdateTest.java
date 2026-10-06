package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class AnalyzerMappingUpdateTest {

    @Test
    public void aSavedRecordKeepsItsSubIdentityAndCallTargetFromTheEditorsRequest() throws Exception {
        AnalyzerMappingUpdate update = new ObjectMapper().readValue("""
                {"baseMappingFingerprint":"sha256:abc",
                 "tests":[{"sourceRowKey":"HIVVL","subIdentity":"","mappingState":"BOUND","testId":"9801",
                           "componentId":null,"callComponentId":"comp-call"},
                          {"sourceRowKey":"HIVVL","subIdentity":"&LOG","mappingState":"BOUND","testId":"9801",
                           "componentId":"comp-LOG","callComponentId":null}],
                 "results":[{"sourceRowKey":"HIVVL","subIdentity":"&LOG","rawValue":"3.00",
                             "mappingState":"UNRESOLVED","testResultId":null}]}
                """, AnalyzerMappingUpdate.class);

        AnalyzerMappingDraft draft = update.toDraft();
        assertEquals("comp-call", draft.tests().get(0).callComponentId());
        assertEquals(new AnalyzerMappingRowKey("HIVVL", "&LOG"), draft.tests().get(1).rowKey());
        assertEquals("comp-LOG", draft.tests().get(1).componentId());
        assertEquals(new AnalyzerMappingRowKey("HIVVL", "&LOG"), draft.results().get(0).rowKey());
    }
}
