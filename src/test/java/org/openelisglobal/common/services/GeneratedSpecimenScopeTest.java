package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;

/**
 * Where a generated result belongs.
 *
 * <p>
 * A reflex or calculation names the specimen its result is reported on, but
 * both engines attached the generated analysis to the sample item of the result
 * that triggered them. A rule configured "trigger on Serum, report on Plasma"
 * reported on Serum, because Serum is what fed it.
 *
 * <p>
 * The order already holds the specimens that were collected for it, so the
 * right one is found among those. What must not happen is a specimen being
 * invented: an order asserting a collection that never took place feeds
 * collection lists, storage and reporting.
 */
@RunWith(MockitoJUnitRunner.class)
public class GeneratedSpecimenScopeTest {

    private static final String SAMPLE_ID = "100";

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private TestResultComponentService testResultComponentService;

    @InjectMocks
    private RuleResultScope scope;

    private Sample sample;

    @Before
    public void setUp() {
        sample = new Sample();
        sample.setId(SAMPLE_ID);
    }

    private SampleItem item(String id, String typeOfSampleId) {
        SampleItem sampleItem = mock(SampleItem.class);
        lenient().when(sampleItem.getId()).thenReturn(id);
        lenient().when(sampleItem.getTypeOfSampleId()).thenReturn(typeOfSampleId);
        return sampleItem;
    }

    private void orderHolds(SampleItem... items) {
        when(sampleItemService.getSampleItemsBySampleId(SAMPLE_ID)).thenReturn(Arrays.asList(items));
    }

    @Test
    public void picksTheOrdersSpecimenOfTheConfiguredType() {
        // Serum triggered it; the rule reports on Plasma, and the order holds
        // both.
        SampleItem serum = item("si-1", "30");
        SampleItem plasma = item("si-2", "31");
        orderHolds(serum, plasma);

        assertEquals(plasma, scope.sampleItemForTarget(sample, "31"));
    }

    @Test
    public void doesNotFallBackToAnotherSpecimenWhenTheConfiguredOneIsAbsent() {
        // The order was never given a Plasma specimen. Answering with the Serum
        // one would report the result against a specimen it did not come from;
        // answering with nothing leaves the caller to keep its behaviour.
        orderHolds(item("si-1", "30"));

        assertNull(scope.sampleItemForTarget(sample, "31"));
    }

    @Test
    public void isUnscopedWhenTheRuleNamesNoSpecimen() {
        assertNull(scope.sampleItemForTarget(sample, null));
        assertNull(scope.sampleItemForTarget(sample, ""));
    }

    @Test
    public void survivesAnOrderWithNoSpecimensAtAll() {
        when(sampleItemService.getSampleItemsBySampleId(SAMPLE_ID)).thenReturn(new ArrayList<>());
        assertNull(scope.sampleItemForTarget(sample, "31"));

        when(sampleItemService.getSampleItemsBySampleId(SAMPLE_ID)).thenReturn(null);
        assertNull(scope.sampleItemForTarget(sample, "31"));
    }

    @Test
    public void survivesWithoutASample() {
        assertNull(scope.sampleItemForTarget(null, "31"));
    }

    @Test
    public void picksTheOnlyMatchWhenTheOrderRepeatsOtherTypes() {
        List<SampleItem> items = Collections.singletonList(item("si-9", "26"));
        when(sampleItemService.getSampleItemsBySampleId(SAMPLE_ID)).thenReturn(items);

        assertEquals(items.get(0), scope.sampleItemForTarget(sample, "26"));
        assertNull("another type of the same order is not a match", scope.sampleItemForTarget(sample, "30"));
    }
}
