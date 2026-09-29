package org.openelisglobal.security;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.rest.provider.SampleEntryTestsForTypeProviderRestController;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The order-entry test picker ({@code GET /rest/sample-type-tests}) must be
 * reachable with {@code PRIV_CATALOGUE_VIEW} alone.
 *
 * <p>
 * It is the single most important read an order-entry role makes, without it
 * the picker shows no tests and no order can be placed, and it used to be
 * reached by bypassing authorization in system context. This exercises the
 * catalogue reads it makes under exactly Reception's authorities, so a gate
 * that still demands an administrative privilege fails here with the offending
 * expression in the message, rather than as an opaque 403 in the browser.
 */
public class SampleTypeTestsCatalogueAccessTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;

    private void authenticateAsCatalogueReader() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reception", "n/a", SeededRoleAuthorities.role("Reception")));
    }

    @Test
    public void sampleTypeCatalogueIsReadableWithCatalogueViewAlone() {
        authenticateAsCatalogueReader();
        assertNotNull("the sample-type catalogue must be readable with catalogue:view",
                typeOfSampleService.getAllTypeOfSamples());
    }

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private TestMethodService testMethodService;

    @Autowired
    private TestService testService;

    @Autowired
    private org.openelisglobal.panel.service.PanelService panelService;

    @Autowired
    private org.openelisglobal.dictionary.service.DictionaryService dictionaryService;

    @Autowired
    private SampleEntryTestsForTypeProviderRestController controller;

    /**
     * The whole picker assembly, through the controller, exactly as the browser
     * reaches it. A gate anywhere in the chain surfaces here as AccessDenied with
     * the failing expression, instead of an opaque 403.
     */
    @Test
    public void theTestPickerAssemblesWithCatalogueViewAlone() throws Exception {
        authenticateAsCatalogueReader();
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setParameter("sampleType", "2");
        assertNotNull("the picker must assemble with catalogue:view",
                controller.processRequest(request, new org.springframework.mock.web.MockHttpServletResponse()));
    }

    @Test
    public void testSectionLookupIsReadableWithCatalogueViewAlone() {
        authenticateAsCatalogueReader();
        // addTests() resolves the "user" bench section for every row.
        testSectionService.getTestSectionByName("user");
    }

    @Test
    public void linkedMethodsAreReadableWithCatalogueViewAlone() {
        authenticateAsCatalogueReader();
        assertNotNull(testMethodService.getLinkedMethodDtos("1"));
    }

    /**
     * The per-test decoration the picker applies to EVERY row it returns: result
     * type, linked methods, QC-threshold flag and bench section.
     *
     * <p>
     * This is asserted directly rather than only through the controller, because
     * the controller filters its test list by the caller's test sections, with none
     * resolved, the list is empty and the per-test loop never runs, so a gate here
     * would pass unnoticed. That is exactly how the first version of this test went
     * green while the live endpoint still returned 403.
     */
    @Test
    public void perTestDecorationIsReadableWithCatalogueViewAlone() {
        authenticateAsCatalogueReader();
        List<org.openelisglobal.test.valueholder.Test> tests = typeOfSampleService.getActiveTestsBySampleTypeId("2",
                true);
        org.junit.Assume.assumeFalse("needs at least one active test on sample type 2", tests.isEmpty());
        for (org.openelisglobal.test.valueholder.Test test : tests) {
            testService.getResultType(test);
            testMethodService.getLinkedMethodDtos(test.getId());
        }
    }

    /**
     * {@code panelService.getPanelById} carries no method-level gate, so it falls
     * through to {@code CrudGate.read}, which evaluates the interface's type-level
     * expression. That expression is now a two-value {@code hasAnyAuthority}, a
     * shape CrudGate had not previously had to evaluate.
     */
    @Test
    public void inheritedCrudReadResolvesATwoValueTypeLevelGate() {
        authenticateAsCatalogueReader();
        panelService.getPanelById("1");
    }

    /**
     * The acceptance checklist the QA Review step renders. It reaches
     * DictionaryService, which in turn calls DictionaryCategoryService, a NESTED
     * gate, invisible from the call site, and the second one of those to deny after
     * the interface gates all looked correct (the first was
     * TestService.getResultType -> TestResultService). Grepping call sites does not
     * find these; only executing the path does.
     */
    @Test
    public void acceptanceChecklistLookupIsReadableWithCatalogueViewAlone() {
        authenticateAsCatalogueReader();
        assertNotNull("dictionary entries by category name must be readable with catalogue:view",
                dictionaryService.getActiveSortedEntriesByCategoryName("sampleRejectionReasons"));
    }

    @Test
    public void panelsForASampleTypeAreReadableWithCatalogueViewAlone() {
        authenticateAsCatalogueReader();
        // The call the test picker makes for each sample type. Gated at type level on
        // TypeOfSamplePanelService, which is why widening only the per-method gates
        // left the picker denied.
        assertNotNull("the sample-type/panel junction must be readable with catalogue:view",
                typeOfSamplePanelService.getTypeOfSamplePanelsForSampleType("2"));
    }
}
