package org.openelisglobal.patientrelation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patientrelation.service.PatientRelationService;
import org.openelisglobal.patientrelation.valueholder.PatientRelation;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientRelationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientRelationService service;

    @Before
    public void init() throws Exception {

        executeDataSetWithStateManagement("testdata/patientrelation.xml");
    }

    @Test
    public void saveSinglePatientRelation_shouldSaveAndReturnRelation() {

        PatientRelation relation = new PatientRelation();

        relation.setPatientIdSource("2001");
        relation.setPatientId("2002");
        relation.setRelation("F");

        PatientRelation saved = service.save(relation);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("2001", saved.getPatientIdSource());
        assertEquals("2002", saved.getPatientId());
        assertEquals("F", saved.getRelation());
    }

    @Test
    public void getPatientRelation_shouldReturnCorrectRelation() {
        PatientRelation loaded = service.get("1");

        assertNotNull(loaded);
        assertEquals("2001", loaded.getPatientIdSource());
        assertEquals("2002", loaded.getPatientId());
        assertEquals("F", loaded.getRelation());
    }

    @Test
    public void updatePatientRelation_shouldModifyAndReturnUpdatedRelation() {

        PatientRelation relation = service.get("1");
        assertNotNull(relation);
        assertEquals("F", relation.getRelation());

        relation.setRelation("M");
        PatientRelation updated = service.update(relation);

        assertNotNull(updated);
        assertEquals("M", updated.getRelation());
        PatientRelation refetched = service.get("1");
        assertEquals("M", refetched.getRelation());
    }

    @Test
    public void updatePatientRelation_shouldUpdateOnlyRelationField() {
        PatientRelation relation = service.get("1");
        assertNotNull(relation);
        String originalSourceId = relation.getPatientIdSource();
        String originalTargetId = relation.getPatientId();

        relation.setRelation("C");
        PatientRelation updated = service.update(relation);

        assertEquals("C", updated.getRelation());
        assertEquals(originalSourceId, updated.getPatientIdSource());
        assertEquals(originalTargetId, updated.getPatientId());
    }
}
