package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

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

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private org.openelisglobal.common.services.IStatusService statusService;

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

    /**
     * The rule names the specimen, so the order gets it.
     *
     * <p>
     * A calculation configured to report AAA on DBS ran on an order holding only
     * Dry Tube and Respiratory Swab, and the result was filed against the
     * Respiratory Swab that triggered it. The configured specimen is the lab's
     * instruction about where the result belongs, so the order is given one rather
     * than the instruction being quietly dropped.
     */
    @Test
    public void addsTheConfiguredSpecimenWhenTheOrderLacksIt() {
        orderHolds(item("si-1", "30"));
        TypeOfSample dbs = new TypeOfSample();
        dbs.setId("26");
        when(typeOfSampleService.get("26")).thenReturn(dbs);

        SampleItem created = scope.resolveOrCreateSampleItemForTarget(sample, "26", "1");

        assertEquals("the generated test reports on the specimen the rule named", dbs, created.getTypeOfSample());
        assertEquals(sample, created.getSample());
        verify(sampleItemService).insert(created);
    }

    @Test
    public void doesNotDuplicateASpecimenTheOrderAlreadyHolds() {
        SampleItem dbs = item("si-7", "26");
        orderHolds(item("si-1", "30"), dbs);

        assertEquals(dbs, scope.resolveOrCreateSampleItemForTarget(sample, "26", "1"));
        verify(sampleItemService, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void addsNothingWhenTheRuleNamesNoSpecimen() {
        assertNull(scope.resolveOrCreateSampleItemForTarget(sample, null, "1"));
        verify(sampleItemService, never()).insert(org.mockito.ArgumentMatchers.any());
    }
}
