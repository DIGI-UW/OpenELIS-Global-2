package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;
import org.openelisglobal.test.service.LegacyTestVariantFinder;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;

/**
 * A configuration file names a test the way the catalogue shows it. A test
 * created in the Test Catalogue Editor keeps that name in its localised name
 * and in {@code test.name} while {@code test.description} holds the editor's
 * free-text description, so a lookup by description alone never finds it and
 * every file that names such a test queues an unresolved reference
 * (release-test run 2026-10-19, "test 'RT Calc Result' not found").
 */
@RunWith(MockitoJUnitRunner.class)
public class CatalogReferenceResolverTest {

    @Mock
    private TestService testService;

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private UnitOfMeasureService unitOfMeasureService;

    @Mock
    private TestResultComponentService componentService;

    @Mock
    private LegacyTestVariantFinder legacyVariantFinder;

    @Mock
    private ReferenceAliasService aliasService;

    @InjectMocks
    private CatalogReferenceResolver resolver;

    private org.openelisglobal.test.valueholder.Test editorCreatedTest;
    private TypeOfSample serum;
    private TypeOfSample plasma;

    @Before
    public void setUp() {
        editorCreatedTest = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(editorCreatedTest.getId()).thenReturn("403");
        serum = specimen("4", "Serum");
        plasma = specimen("5", "Plasma");
        lenient().when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Arrays.asList(serum, plasma));
        lenient().when(typeOfSampleService.getTypeOfSampleForTest("403")).thenReturn(Collections.singletonList(serum));
        lenient().when(testService.getTestByDescription(anyString())).thenReturn(null);
        lenient().when(testService.getTestByNormalizedDescription(anyString())).thenReturn(null);
        lenient().when(testService.getTestByLocalCode(anyString())).thenReturn(null);
        lenient().when(testService.getActiveTestByName(anyString())).thenReturn(Collections.emptyList());
        lenient().when(aliasService.resolve(anyString(), anyString())).thenReturn(null);
        lenient().when(legacyVariantFinder.find(anyString())).thenReturn(Collections.emptyList());
    }

    @Test
    public void aTestNamedByItsCatalogueNameIsFoundWhenItsDescriptionDiffers() {
        lenient().when(testService.getActiveTestByName("RT Calc Result"))
                .thenReturn(Collections.singletonList(editorCreatedTest));

        assertSame(editorCreatedTest, resolver.resolveTest("RT Calc Result", "Serum", "rt-reflex-rules-2.csv:2"));
    }

    @Test
    public void aTestNamedWithoutASpecimenIsFoundByItsCatalogueName() {
        lenient().when(testService.getActiveTestByName("RT Calc Result"))
                .thenReturn(Collections.singletonList(editorCreatedTest));

        assertSame(editorCreatedTest, resolver.resolveTest("RT Calc Result", "", "rt-reflex-rules-2.csv:2"));
    }

    @Test
    public void aNamesakeNotOfferedOnTheRowsSpecimenIsNotBound() {
        org.openelisglobal.test.valueholder.Test glucosePlasma = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(glucosePlasma.getId()).thenReturn("120");
        lenient().when(testService.getActiveTestByName("Glucose")).thenReturn(Collections.singletonList(glucosePlasma));
        lenient().when(typeOfSampleService.getTypeOfSampleForTest("120")).thenReturn(Collections.singletonList(plasma));

        assertNull(resolver.resolveTest("Glucose", "Serum", "rt-tests.csv:7"));
    }

    @Test
    public void aRememberedAliasWinsOverTheNameLookup() {
        org.openelisglobal.test.valueholder.Test aliasedTest = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(aliasService.resolve(UnresolvedReference.TYPE_TEST, "RT Calc Result")).thenReturn("77");
        lenient().when(testService.get("77")).thenReturn(aliasedTest);
        lenient().when(testService.getActiveTestByName("RT Calc Result"))
                .thenReturn(Collections.singletonList(editorCreatedTest));

        assertSame(aliasedTest, resolver.resolveTest("RT Calc Result", "Serum", "rt-reflex-rules-2.csv:2"));
    }

    @Test
    public void aTestKnownByDescriptionStillWinsOverTheNameLookup() {
        org.openelisglobal.test.valueholder.Test loaderCreatedTest = mock(
                org.openelisglobal.test.valueholder.Test.class);
        lenient().when(testService.getTestByDescription("RT Glucose Dual")).thenReturn(loaderCreatedTest);
        lenient().when(testService.getActiveTestByName("RT Glucose Dual"))
                .thenReturn(Collections.singletonList(editorCreatedTest));

        assertSame(loaderCreatedTest, resolver.resolveTest("RT Glucose Dual", "Serum", "rt-tests.csv:2"));
    }

    @Test
    public void anUnknownNameStillResolvesToNothing() {
        assertNull(resolver.resolveTest("RT Nowhere", "Serum", "rt-reflex-rules-2.csv:3"));
    }

    private TypeOfSample specimen(String id, String name) {
        TypeOfSample sampleType = mock(TypeOfSample.class);
        lenient().when(sampleType.getId()).thenReturn(id);
        lenient().when(sampleType.getLocalizedName()).thenReturn(name);
        lenient().when(sampleType.getDescription()).thenReturn(name);
        return sampleType;
    }
}
