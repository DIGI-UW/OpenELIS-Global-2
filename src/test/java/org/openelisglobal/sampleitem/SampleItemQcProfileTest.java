package org.openelisglobal.sampleitem;

import static org.junit.Assert.*;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.valueholder.SampleItemQcProfile;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for SampleItemService.getQcProfile() (OGC-554).
 *
 * Test data in testdata/qc-evaluation.xml: - sample_item id=1: regular (no QC
 * profile) - sample_item id=2: BLANK QC profile - sample_item id=3: DUPLICATE
 * QC profile (parent=1) - sample_item id=4: CONTROL QC profile
 * (expectedValue=50)
 */
public class SampleItemQcProfileTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleItemService sampleItemService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qc-evaluation.xml");
    }

    @Test
    public void getQcProfile_regularSampleItem_returnsEmpty() {
        Optional<SampleItemQcProfile> profile = sampleItemService.getQcProfile("1");

        assertFalse("Regular sample item should have no QC profile", profile.isPresent());
    }

    @Test
    public void getQcProfile_blankSampleItem_returnsBlankProfile() {
        Optional<SampleItemQcProfile> profile = sampleItemService.getQcProfile("2");

        assertTrue("BLANK sample item should have a QC profile", profile.isPresent());
        assertEquals("BLANK", profile.get().getQcType());
        assertNull("BLANK should not have a parent", profile.get().getParentSampleItemId());
        assertNull("BLANK should not have an expected value", profile.get().getExpectedValue());
    }

    @Test
    public void getQcProfile_duplicateSampleItem_returnsProfileWithParent() {
        Optional<SampleItemQcProfile> profile = sampleItemService.getQcProfile("3");

        assertTrue("DUPLICATE sample item should have a QC profile", profile.isPresent());
        assertEquals("DUPLICATE", profile.get().getQcType());
        assertEquals("Parent should reference sample item 1", Integer.valueOf(1),
                profile.get().getParentSampleItemId());
    }

    @Test
    public void getQcProfile_controlSampleItem_returnsProfileWithExpectedValue() {
        Optional<SampleItemQcProfile> profile = sampleItemService.getQcProfile("4");

        assertTrue("CONTROL sample item should have a QC profile", profile.isPresent());
        assertEquals("CONTROL", profile.get().getQcType());
        assertNotNull("CONTROL should have an expected value", profile.get().getExpectedValue());
        assertEquals("Expected value should be 50", 0,
                profile.get().getExpectedValue().compareTo(new java.math.BigDecimal("50.00000")));
    }

    @Test
    public void getQcProfile_nonExistentSampleItem_returnsEmpty() {
        Optional<SampleItemQcProfile> profile = sampleItemService.getQcProfile("999");

        assertFalse("Non-existent sample item should return empty", profile.isPresent());
    }
}
