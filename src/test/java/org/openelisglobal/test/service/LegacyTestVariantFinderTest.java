package org.openelisglobal.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.test.service.LegacyTestVariantFinder.LegacyTestVariant;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public class LegacyTestVariantFinderTest {

    private final LegacyTestVariantFinder finder = new LegacyTestVariantFinder();
    private final TestService testService = mock(TestService.class);
    private final TypeOfSampleService typeOfSampleService = mock(TypeOfSampleService.class);

    @Before
    public void setUp() throws Exception {
        inject("testService", testService);
        inject("typeOfSampleService", typeOfSampleService);
        TypeOfSample serum = sampleType("2", "Serum");
        TypeOfSample wholeBlood = sampleType("4", "Whole Blood");
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(List.of(serum, wholeBlood));
    }

    @Test
    public void find_matchesTheNameInItsNormalizedFormAndKeepsTheSpecimenLabel() {
        org.openelisglobal.test.valueholder.Test seeded = test("313", "HIVVIRALLOAD(Serum)");
        org.openelisglobal.test.valueholder.Test spaced = test("402", "HIV Viral Load(Whole Blood)");
        when(testService.getTestsByNormalizedDescriptionPrefix("HIV Viral Load")).thenReturn(List.of(seeded, spaced));

        List<LegacyTestVariant> variants = finder.find("HIV Viral Load");

        assertEquals(2, variants.size());
        assertSame(seeded, variants.get(0).test());
        assertEquals("2", variants.get(0).specimen().getId());
        assertEquals("Serum", variants.get(0).specimenLabel());
        assertSame(spaced, variants.get(1).test());
        assertEquals("Whole Blood", variants.get(1).specimenLabel());
    }

    @Test
    public void find_ignoresPlainRecordsUnknownSpecimensAndOtherBaseNames() {
        org.openelisglobal.test.valueholder.Test plain = test("1", "HIV Viral Load");
        org.openelisglobal.test.valueholder.Test fasting = test("2", "HIV Viral Load(fasting)");
        org.openelisglobal.test.valueholder.Test reflex = test("3", "HIV Viral Load Reflex(Serum)");
        org.openelisglobal.test.valueholder.Test variant = test("4", "hiv viral load (Serum)");
        when(testService.getTestsByNormalizedDescriptionPrefix("HIV Viral Load"))
                .thenReturn(List.of(plain, fasting, reflex, variant));

        List<LegacyTestVariant> variants = finder.find("HIV Viral Load");

        assertEquals(1, variants.size());
        assertSame(variant, variants.get(0).test());
    }

    @Test
    public void find_readsTheSpecimenFromTheLastParenthesisedPart() {
        org.openelisglobal.test.valueholder.Test percent = test("7", "Lymphocytes (%)(Whole Blood)");
        when(testService.getTestsByNormalizedDescriptionPrefix("Lymphocytes (%)")).thenReturn(List.of(percent));

        List<LegacyTestVariant> variants = finder.find("Lymphocytes (%)");

        assertEquals(1, variants.size());
        assertEquals("4", variants.get(0).specimen().getId());
    }

    @Test
    public void find_neverQueriesForANameThatNormalizesToNothing() {
        assertTrue(finder.find("(%)").isEmpty());
        verify(testService, never()).getTestsByNormalizedDescriptionPrefix("(%)");
    }

    private static org.openelisglobal.test.valueholder.Test test(String id, String description) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setDescription(description);
        return test;
    }

    private static TypeOfSample sampleType(String id, String description) {
        TypeOfSample sampleType = new TypeOfSample();
        sampleType.setId(id);
        sampleType.setDescription(description);
        return sampleType;
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = LegacyTestVariantFinder.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(finder, value);
    }
}
