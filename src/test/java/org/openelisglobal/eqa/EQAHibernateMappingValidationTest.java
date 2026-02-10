package org.openelisglobal.eqa;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAPriority;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.openelisglobal.eqa.valueholder.SampleEQA;

/**
 * ORM validation test for EQA entities. Validates all 5 entities can be
 * instantiated and have correct defaults. Constitution V.4: Must execute in
 * less than 5 seconds, NO database connection required.
 */
public class EQAHibernateMappingValidationTest {

    @Test
    public void testEQAProgramEntityCanBeInstantiated() {
        EQAProgram program = new EQAProgram();
        assertNotNull("EQAProgram should be instantiable", program);
        assertNotNull("isActive should default to true", program.getIsActive());
        org.junit.Assert.assertTrue("isActive default should be true", program.getIsActive());
    }

    @Test
    public void testEQAProgramTestEntityCanBeInstantiated() {
        EQAProgramTest programTest = new EQAProgramTest();
        assertNotNull("EQAProgramTest should be instantiable", programTest);
        assertNotNull("isActive should default to true", programTest.getIsActive());
        org.junit.Assert.assertTrue("isActive default should be true", programTest.getIsActive());
    }

    @Test
    public void testEQADistributionEntityCanBeInstantiated() {
        EQADistribution distribution = new EQADistribution();
        assertNotNull("EQADistribution should be instantiable", distribution);
        assertNotNull("status should default to DRAFT", distribution.getStatus());
        org.junit.Assert.assertEquals("status default should be DRAFT", EQADistributionStatus.DRAFT,
                distribution.getStatus());
    }

    @Test
    public void testEQAResultEntityCanBeInstantiated() {
        EQAResult result = new EQAResult();
        assertNotNull("EQAResult should be instantiable", result);
        assertNotNull("isLateSubmission should default to false", result.getIsLateSubmission());
        org.junit.Assert.assertFalse("isLateSubmission default should be false", result.getIsLateSubmission());
    }

    @Test
    public void testSampleEQAEntityCanBeInstantiated() {
        SampleEQA sampleEqa = new SampleEQA();
        assertNotNull("SampleEQA should be instantiable", sampleEqa);
        assertNotNull("isEqaSample should default to false", sampleEqa.getIsEqaSample());
        org.junit.Assert.assertFalse("isEqaSample default should be false", sampleEqa.getIsEqaSample());
        assertNotNull("eqaPriority should default to STANDARD", sampleEqa.getEqaPriority());
        org.junit.Assert.assertEquals("eqaPriority default should be STANDARD", EQAPriority.STANDARD,
                sampleEqa.getEqaPriority());
    }

    @Test
    public void testAllEnumsHaveExpectedValues() {
        // EQAPriority
        org.junit.Assert.assertEquals(3, EQAPriority.values().length);
        assertNotNull(EQAPriority.valueOf("STANDARD"));
        assertNotNull(EQAPriority.valueOf("URGENT"));
        assertNotNull(EQAPriority.valueOf("CRITICAL"));

        // EQADistributionStatus
        org.junit.Assert.assertEquals(4, EQADistributionStatus.values().length);
        assertNotNull(EQADistributionStatus.valueOf("DRAFT"));
        assertNotNull(EQADistributionStatus.valueOf("PREPARED"));
        assertNotNull(EQADistributionStatus.valueOf("SHIPPED"));
        assertNotNull(EQADistributionStatus.valueOf("COMPLETED"));

        // EQASubmissionMethod
        org.junit.Assert.assertEquals(3, EQASubmissionMethod.values().length);
        assertNotNull(EQASubmissionMethod.valueOf("FHIR"));
        assertNotNull(EQASubmissionMethod.valueOf("MANUAL"));
        assertNotNull(EQASubmissionMethod.valueOf("FILE_UPLOAD"));

        // EQAPerformanceStatus
        org.junit.Assert.assertEquals(3, EQAPerformanceStatus.values().length);
        assertNotNull(EQAPerformanceStatus.valueOf("ACCEPTABLE"));
        assertNotNull(EQAPerformanceStatus.valueOf("QUESTIONABLE"));
        assertNotNull(EQAPerformanceStatus.valueOf("UNACCEPTABLE"));
    }

    @Test
    public void testEQAProgramPrePersistGeneratesUuid() {
        EQAProgram program = new EQAProgram();
        org.junit.Assert.assertNull("fhirUuid should be null before prePersist", program.getFhirUuid());
        program.prePersist();
        assertNotNull("fhirUuid should be set after prePersist", program.getFhirUuid());
    }

    @Test
    public void testEQADistributionPrePersistGeneratesUuid() {
        EQADistribution distribution = new EQADistribution();
        org.junit.Assert.assertNull("fhirUuid should be null before prePersist", distribution.getFhirUuid());
        distribution.prePersist();
        assertNotNull("fhirUuid should be set after prePersist", distribution.getFhirUuid());
    }

    @Test
    public void testEQAResultPrePersistGeneratesUuid() {
        EQAResult result = new EQAResult();
        org.junit.Assert.assertNull("fhirUuid should be null before prePersist", result.getFhirUuid());
        result.prePersist();
        assertNotNull("fhirUuid should be set after prePersist", result.getFhirUuid());
    }
}
