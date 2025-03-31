package org.openelisglobal.sampleorganization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleOrganizationServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    SampleOrganizationService sampleOrganizationService;
    @Autowired
    SampleService sampleService;

    private static final String SAMPLE_ENTERED_DATE = "2024-03-05";
    private static final String SAMPLE_ACCESSION_NUMBER = "2000";
    private static final String SAMPLE_RECEIVED_TIMESTAMP = "012/06/2024";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-organization.xml");
    }

    @Test
    public void verifyTestData() {
        List<SampleOrganization> sampleOrganizationList = sampleOrganizationService.getAll();

        assertNotNull("SampleOrganization list should not be null", sampleOrganizationList);
        assertFalse("SampleOrganization list should not be empty", sampleOrganizationList.isEmpty());

        sampleOrganizationList.forEach(sampleOrganization -> {
            assertNotNull("SampleOrganization ID should not be null", sampleOrganization.getId());
            assertNotNull("SampleOrganization Sample should not be null", sampleOrganization.getSample());
            assertNotNull("Sample Accession Number should not be null",
                    sampleOrganization.getSample().getAccessionNumber());
        });
    }

    @Test
    public void createSampleOrganization_shouldCreateNewSampleOrganization() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample", "sample_organization" });
        SampleOrganization sampleOrganization = creatSampleOrganization();

        Assert.assertEquals(0, sampleOrganizationService.getAll().size());

        sampleOrganizationService.insert(sampleOrganization);

        Assert.assertEquals(1, sampleOrganizationService.getAll().size());

        sampleOrganizationService.delete(sampleOrganization);
    }

    @Test
    public void updateSampleOrganization_shouldUpdateSampleOrganization() {
        Sample samp = sampleService.get("1");
        Assert.assertEquals("2", sampleOrganizationService.getDataBySample(samp).getId());

        samp.setAccessionNumber("2222");
        sampleService.save(samp);

        Assert.assertEquals("2222", sampleOrganizationService.getDataBySample(samp).getSample().getAccessionNumber());
    }

    @Test
    public void deleteSampleOrganization_shouldDeleteSampleOrganization() {
        Assert.assertEquals(3, sampleOrganizationService.getAll().size());

        SampleOrganization savedSampleOrganization = sampleOrganizationService.get("1");

        sampleOrganizationService.delete(savedSampleOrganization);

        Assert.assertEquals(2, sampleOrganizationService.getAll().size());

    }

    @Test
    public void getData_shouldReturnCopiedPropertiesFromDatabase() {
        SampleOrganization sampleOrganizationToUpdate = new SampleOrganization();
        sampleOrganizationToUpdate.setId("3");

        sampleOrganizationService.getData(sampleOrganizationToUpdate);
        Assert.assertEquals("52541", sampleOrganizationToUpdate.getSample().getAccessionNumber());
    }

    private SampleOrganization creatSampleOrganization() throws ParseException {
        Sample samp = new Sample();
        samp.setEnteredDate(Date.valueOf(SampleOrganizationServiceTest.SAMPLE_ENTERED_DATE));
        samp.setReceivedTimestamp(new Timestamp(new SimpleDateFormat("dd/MM/yyyy")
                .parse(SampleOrganizationServiceTest.SAMPLE_RECEIVED_TIMESTAMP).getTime()));
        samp.setAccessionNumber(SampleOrganizationServiceTest.SAMPLE_ACCESSION_NUMBER);
        String sampId = sampleService.insert(samp);

        SampleOrganization sampleOrganization = new SampleOrganization();
        sampleOrganization.setSample(sampleService.getSampleByAccessionNumber(sampId));
        sampleOrganization.setLastupdated(new Timestamp(System.currentTimeMillis()));
        return sampleOrganization;
    }
}
