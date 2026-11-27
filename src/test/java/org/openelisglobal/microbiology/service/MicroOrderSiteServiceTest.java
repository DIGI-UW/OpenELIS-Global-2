package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;

public class MicroOrderSiteServiceTest {
    @Test
    public void environmentalWorkUsesTheSpecimenSiteOrTheRecordedOrderSite() {
        var sites = mock(VectorSamplingSiteService.class);
        var observations = mock(ObservationHistoryService.class);
        var service = new MicroOrderSiteService(sites, observations);
        var site = new VectorSamplingSite();
        site.setId(7);
        site.setName("Ward 7");
        when(sites.get(7)).thenReturn(site);
        var order = new Sample();
        order.setId("42");
        order.setDomain("E");
        var test = new org.openelisglobal.test.valueholder.Test();
        var unit = new TestSection();
        unit.setDomain("ENVIRONMENTAL");
        test.setTestSection(unit);
        when(observations.getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_ID, "42")).thenReturn("7");
        assertEquals("7", service.resolve(order, "7", test));
        verifyZeroInteractions(observations);
        assertEquals("7", service.resolve(order, null, test));
        assertThrows(IllegalArgumentException.class, () -> service.resolve(order, "8", test));
        unit.setDomain("CLINICAL");
        assertThrows(IllegalArgumentException.class, () -> service.resolve(order, "7", test));
    }

    @Test
    public void clinicalWorkDoesNotUseSamplingSitesAndMissingEnvironmentalSitesAreRejected() {
        var sites = mock(VectorSamplingSiteService.class);
        var observations = mock(ObservationHistoryService.class);
        var service = new MicroOrderSiteService(sites, observations);
        var order = new Sample();
        order.setDomain("CLINICAL");
        assertNull(service.resolve(order, "7", null));
        verifyZeroInteractions(sites, observations);
        assertThrows(IllegalArgumentException.class, () -> service.siteName(null));
        assertThrows(IllegalArgumentException.class, () -> service.siteName("not-an-id"));
    }
}
