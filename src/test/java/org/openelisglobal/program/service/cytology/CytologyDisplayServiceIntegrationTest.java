package org.openelisglobal.program.service.cytology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.program.valueholder.cytology.CytologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.CytologyDiagnosisResultType;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.DiagnosisCategory;
import org.openelisglobal.program.valueholder.cytology.CytologyDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy.SpecimenAdequancySatisfaction;
import org.springframework.beans.factory.annotation.Autowired;

public class CytologyDisplayServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CytologyDisplayService cytologyDisplayService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/cytology-display-service.xml");
        authenticateAs("technician1");
        jdbcTemplate
                .update("INSERT INTO clinlims.cytology_specimen_adequacy_value (cytology_specimen_adequacy_id, value)"
                        + " VALUES (1, '100')");
        TableIdService.getInstance().REFERRING_ORG_TYPE_ID = "5";
        TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID = "6";
    }

    @Test
    public void convertToCaseDisplayItem_withAllFieldsPopulated_shouldMapCompleteCaseDisplayItem() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(1);

        assertEquals(CytologyStatus.READY_FOR_CYTOPATHOLOGIST, item.getStatus());
        assertEquals("2024-06-03", item.getRequestDate().toString().substring(0, 10));
        assertEquals("Smith,Sarah", item.getAssignedCytoPathologist());
        assertEquals("2", item.getAssignedPathologistId());
        assertEquals("Doe,John", item.getAssignedTechnician());
        assertEquals("1", item.getAssignedTechnicianId());
        assertEquals("Jane", item.getFirstName());
        assertEquals("Doe", item.getLastName());
        assertEquals("12345", item.getLabNumber());
        assertEquals(Integer.valueOf(1), item.getPathologySampleId());
        assertEquals("1", item.getPatientPK());

        assertFalse(item.getProgramQuestionnaire() == null);
        assertEquals("Cervical Screening Questionnaire", item.getProgramQuestionnaire().getName());
        assertFalse(item.getProgramQuestionnaireResponse() == null);
        assertEquals("f1cdeff8-8d5b-4023-bd7c-932b4b98b6d2", item.getProgramQuestionnaireResponse().getId());

        assertEquals(1, item.getSlides().size());
        assertEquals(Integer.valueOf(1), item.getSlides().get(0).getSlideNumber());
        assertEquals("Slide Rack A", item.getSlides().get(0).getLocation());

        assertEquals(1, item.getReports().size());
        assertEquals("CERVICAL_OR_VAGINAL_CYTOLOGY", item.getReports().get(0).getReportType().name());

        assertFalse(item.getSpecimenAdequacy() == null);
        assertEquals(SpecimenAdequancySatisfaction.SATISFACTORY_FOR_EVALUATION,
                item.getSpecimenAdequacy().getSatisfaction());
        assertEquals(1, item.getSpecimenAdequacy().getValues().size());
        assertEquals("100", item.getSpecimenAdequacy().getValues().get(0).getId());
        assertEquals("Adequate cellularity", item.getSpecimenAdequacy().getValues().get(0).getValue());

        assertFalse(item.getDiagnosis() == null);
        assertFalse(item.getDiagnosis().getNegativeDiagnosis());
        List<CytologyCaseViewDisplayItem.Diagnosis.DiagnosisResultsMap> diagnosisMaps = item.getDiagnosis()
                .getDiagnosisResultsMaps();
        assertEquals(1, diagnosisMaps.size());
        assertEquals(DiagnosisCategory.EPITHELIAL_CELL_ABNORMALITY, diagnosisMaps.get(0).getCategory());
        assertEquals(CytologyDiagnosisResultType.DICTIONARY, diagnosisMaps.get(0).getResultType());
        assertEquals(1, diagnosisMaps.get(0).getResults().size());
        assertEquals("101", diagnosisMaps.get(0).getResults().get(0).getId());
        assertEquals("High-grade squamous intraepithelial lesion", diagnosisMaps.get(0).getResults().get(0).getValue());

        assertEquals("Central Clinic", item.getReferringFacility());
        assertEquals("Pathology Department", item.getDepartment());
        assertEquals("Johnson Alice", item.getRequester());

        String expectedAge = DateUtil.getCurrentAgeForDate(Timestamp.valueOf("1990-03-12 00:00:00.0"),
                DateUtil.getNowAsTimestamp());
        assertEquals(expectedAge, item.getAge());
        assertEquals("F", item.getSex());
    }

    @Test
    public void convertToCaseDisplayItem_withTextDiagnosis_shouldMapTextResults() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(2);

        assertEquals(CytologyStatus.COMPLETED, item.getStatus());
        assertFalse(item.getDiagnosis() == null);
        assertTrue(item.getDiagnosis().getNegativeDiagnosis());

        List<CytologyCaseViewDisplayItem.Diagnosis.DiagnosisResultsMap> diagnosisMaps = item.getDiagnosis()
                .getDiagnosisResultsMaps();
        assertEquals(1, diagnosisMaps.size());
        assertEquals(DiagnosisCategory.NON_NEOPLASTIC_CELLULAR_VARIATIONS, diagnosisMaps.get(0).getCategory());
        assertEquals(CytologyDiagnosisResultType.TEXT, diagnosisMaps.get(0).getResultType());
        assertEquals(1, diagnosisMaps.get(0).getResults().size());
        assertEquals("Atypical cells of undetermined significance", diagnosisMaps.get(0).getResults().get(0).getId());
        assertEquals("Atypical cells of undetermined significance",
                diagnosisMaps.get(0).getResults().get(0).getValue());
    }

    @Test
    public void convertToCaseDisplayItem_whenStaffUnassigned_shouldHaveNullPathologistAndTechnician() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(4);

        assertTrue(item.getAssignedCytoPathologist() == null);
        assertTrue(item.getAssignedPathologistId() == null);
        assertTrue(item.getAssignedTechnician() == null);
        assertTrue(item.getAssignedTechnicianId() == null);
    }

    @Test
    public void convertToCaseDisplayItem_whenSpecimenAdequacyIsNull_shouldHaveNullSpecimenAdequacy() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(5);

        assertTrue(item.getSpecimenAdequacy() == null);
    }

    @Test
    public void convertToCaseDisplayItem_whenDiagnosisIsNull_shouldHaveNullDiagnosis() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(5);

        assertTrue(item.getDiagnosis() == null);
    }

    @Test
    public void convertToCaseDisplayItem_whenDiagnosisHasEmptyResultsMaps_shouldHaveEmptyResultsMaps() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(6);

        assertFalse(item.getDiagnosis() == null);
        assertTrue(item.getDiagnosis().getNegativeDiagnosis());
        assertEquals(0, item.getDiagnosis().getDiagnosisResultsMaps().size());
    }

    @Test
    public void convertToCaseDisplayItem_whenReportsAndSlidesEmpty_shouldHandleEmptyCollections() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(4);

        assertEquals(0, item.getSlides().size());
        assertEquals(0, item.getReports().size());
    }

    @Test
    public void convertToCaseDisplayItem_whenNoReferringDepartment_shouldHaveNullDepartment() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(2);

        assertEquals("Central Clinic", item.getReferringFacility());
        assertTrue(item.getDepartment() == null);
        assertEquals("Johnson Alice", item.getRequester());
    }

    @Test
    public void convertToCaseDisplayItem_whenNoRequesterOrReferringFacility_shouldHandleMissingOrderRequesters() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(4);

        assertTrue(item.getReferringFacility() == null);
        assertTrue(item.getDepartment() == null);
        assertEquals("null null", item.getRequester());
    }

    @Test
    public void convertToCaseDisplayItem_whenPatientBirthDateNull_shouldHaveNullAge() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(3);

        assertTrue(item.getAge() == null);
        assertEquals("M", item.getSex());
    }

    @Test
    public void convertToCaseDisplayItem_whenQuestionnaireNotFound_shouldHaveNullQuestionnaire() {
        CytologyCaseViewDisplayItem item = cytologyDisplayService.convertToCaseDisplayItem(2);

        assertTrue(item.getProgramQuestionnaire() == null);
        assertTrue(item.getProgramQuestionnaireResponse() == null);
    }

    @Test
    public void convertToCaseDisplayItem_whenNonExistentSampleId_shouldThrowException() {
        assertThrows(Exception.class, () -> cytologyDisplayService.convertToCaseDisplayItem(999999));
    }

    @Test
    public void convertToCaseDisplayItem_whenNullSampleId_shouldThrowException() {
        assertThrows(Exception.class, () -> cytologyDisplayService.convertToCaseDisplayItem(null));
    }

    @Test
    public void convertToDisplayItem_withAssignedStaff_shouldMapSummaryDisplayItem() {
        CytologyDisplayItem item = cytologyDisplayService.convertToDisplayItem(1);

        assertEquals(CytologyStatus.READY_FOR_CYTOPATHOLOGIST, item.getStatus());
        assertEquals("2024-06-03", item.getRequestDate().toString().substring(0, 10));
        assertEquals("Smith,Sarah", item.getAssignedCytoPathologist());
        assertEquals("Doe,John", item.getAssignedTechnician());
        assertEquals("Jane", item.getFirstName());
        assertEquals("Doe", item.getLastName());
        assertEquals("12345", item.getLabNumber());
        assertEquals(Integer.valueOf(1), item.getPathologySampleId());
    }

    @Test
    public void convertToDisplayItem_whenStaffUnassigned_shouldHaveNullPathologistAndTechnician() {
        CytologyDisplayItem item = cytologyDisplayService.convertToDisplayItem(4);

        assertEquals(CytologyStatus.PREPARING_SLIDES, item.getStatus());
        assertTrue(item.getAssignedCytoPathologist() == null);
        assertTrue(item.getAssignedTechnician() == null);
        assertEquals("Bob", item.getFirstName());
        assertEquals("Brown", item.getLastName());
        assertEquals("12348", item.getLabNumber());
        assertEquals(Integer.valueOf(4), item.getPathologySampleId());
    }

    @Test
    public void convertToDisplayItem_whenNonExistentSampleId_shouldThrowException() {
        assertThrows(Exception.class, () -> cytologyDisplayService.convertToDisplayItem(999999));
    }

    @Test
    public void convertToDisplayItem_whenNullSampleId_shouldThrowException() {
        assertThrows(Exception.class, () -> cytologyDisplayService.convertToDisplayItem(null));
    }

    @Test
    public void getCytologySampleWithLoadedAttributes_withDiagnosis_shouldEagerlyLoadDiagnosisResultsMaps() {
        CytologySample sample = cytologyDisplayService.getCytologySampleWithLoadedAttributes(1);

        assertEquals(Integer.valueOf(1), sample.getId());
        assertFalse(sample.getDiagnosis() == null);
        assertEquals(1, sample.getDiagnosis().getDiagnosisResultsMaps().size());
        assertEquals(DiagnosisCategory.EPITHELIAL_CELL_ABNORMALITY,
                sample.getDiagnosis().getDiagnosisResultsMaps().get(0).getCategory());
    }

    @Test
    public void getCytologySampleWithLoadedAttributes_whenDiagnosisIsNull_shouldReturnSampleWithoutError() {
        CytologySample sample = cytologyDisplayService.getCytologySampleWithLoadedAttributes(5);

        assertEquals(Integer.valueOf(5), sample.getId());
        assertTrue(sample.getDiagnosis() == null);
    }

    @Test
    public void getCytologySampleWithLoadedAttributes_whenNonExistentSampleId_shouldThrowException() {
        assertThrows(Exception.class, () -> cytologyDisplayService.getCytologySampleWithLoadedAttributes(999999));
    }

    @Test
    public void getCytologySampleWithLoadedAttributes_whenNullSampleId_shouldThrowException() {
        assertThrows(Exception.class, () -> cytologyDisplayService.getCytologySampleWithLoadedAttributes(null));
    }
}
