package org.openelisglobal.sampleacceptance.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.sampleacceptance.service.ResampleResult;
import org.openelisglobal.sampleacceptance.service.ResampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;

/**
 * The acceptance evaluation names a resample's linked order by lab number in
 * both directions, never by internal sample id (OGC-1269).
 */
@Rollback
public class SampleAcceptanceRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String ORIGINAL_SAMPLE_ID = "1";
    private static final String ORIGINAL_ACCESSION = "24-00001";
    private static final String REPLACEMENT_ACCESSION = "24-00002";

    @Autowired
    private SampleAcceptanceRestController controller;

    @Autowired
    private ResampleService resampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private AccessionNumberValidatorFactory accessionNumberValidatorFactory;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/resample-workflow.xml");
        statusService.refreshCache();
        IAccessionNumberGenerator generator = mock(IAccessionNumberGenerator.class);
        when(generator.getNextAvailableAccessionNumber(any(), anyBoolean())).thenReturn(REPLACEMENT_ACCESSION);
        when(accessionNumberValidatorFactory.getGenerator(AccessionFormat.MAIN)).thenReturn(generator);
    }

    @After
    public void resetAccessionFactory() {
        reset(accessionNumberValidatorFactory);
    }

    @Test
    public void theEvaluationNamesBothLinkedOrdersByLabNumberAfterAResample() {
        String originalItemId = sampleItemService.getSampleItemsBySampleId(ORIGINAL_SAMPLE_ID).get(0).getId();

        ResampleResult result = resampleService.resample(originalItemId, "Sample volume inadequate", 1);

        Map<String, Object> original = resampleBlock(originalItemId);
        assertEquals(result.getNewSampleId(), original.get("resampledToSampleId"));
        assertEquals(REPLACEMENT_ACCESSION, original.get("resampledToAccession"));

        String replacementItemId = sampleItemService.getSampleItemsBySampleId(result.getNewSampleId()).get(0).getId();
        Map<String, Object> replacement = resampleBlock(replacementItemId);
        assertEquals(ORIGINAL_SAMPLE_ID, replacement.get("resampledFromSampleId"));
        assertEquals(ORIGINAL_ACCESSION, replacement.get("resampledFromAccession"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resampleBlock(String sampleItemId) {
        ResponseEntity<?> response = controller.getForSampleItem(sampleItemId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return (Map<String, Object>) ((Map<String, Object>) response.getBody()).get("resample");
    }
}
