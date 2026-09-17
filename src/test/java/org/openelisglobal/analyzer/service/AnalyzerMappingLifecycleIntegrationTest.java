package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.PROFILE_ID;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.PROFILE_REVISION;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.RECOGNITION_FINGERPRINT;

import ca.uhn.fhir.context.FhirContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingConfirmationDAO;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real local mapping, catalog, user, import and history services; Bridge
 * catalog is the external fixture.
 */
@Transactional
public class AnalyzerMappingLifecycleIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String SOURCE_CODE = "VENDOR-NEW-42";
    private static final String RAW_VALUE = "DETECTED";
    private static final String EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-";

    @Autowired
    private AnalyzerInstanceLocalStateService instances;
    @Autowired
    private AnalyzerService analyzers;
    @Autowired
    private AnalyzerTypeMappingService mappings;
    @Autowired
    private AnalyzerSiteBindingService bindings;
    @Autowired
    private AnalyzerSiteBindingConfirmationDAO confirmations;
    @Autowired
    private AnalyzerNormalizedResultImportService imports;
    @Autowired
    private TestService tests;
    @Autowired
    private TestSectionService labUnits;
    @Autowired
    private LocalizationService localizations;
    @Autowired
    private TestResultService options;
    @Autowired
    private HistoryService history;
    @Autowired
    private ReferenceTablesService referenceTables;
    @Autowired
    private AnalyzerResultsService results;
    @Autowired
    private DictionaryService dictionaries;
    @Autowired
    private DictionaryCategoryService dictionaryCategories;
    @Autowired
    private FhirContext fhirContext;
    @PersistenceContext
    private EntityManager entityManager;

    private String testId;
    private String resultOptionId;
    private String positiveValue;
    private String labUnitId;
    private AnalyzerInstanceState analyzer;
    private String originalRevisionId;

    @Before
    public void createOwnedCatalogAndAnalyzer() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setName("Mapping lifecycle test");
        test.setDescription("Mapping lifecycle test");
        test.setGuid(UUID.randomUUID().toString());
        test.setIsActive("Y");
        test.setIsReportable("Y");
        test.setSysUserId(TEST_SYS_USER_ID);
        testId = tests.insert(test);
        DictionaryCategory category = new DictionaryCategory();
        category.setCategoryName("Lifecycle result values");
        category.setDescription("Owned by the mapping lifecycle test");
        category.setLocalAbbreviation("LIFECYCLE");
        category.setSysUserId(TEST_SYS_USER_ID);
        dictionaryCategories.insert(category);
        Dictionary positive = new Dictionary();
        positive.setDictionaryCategory(category);
        positive.setDictEntry("Positive");
        positive.setIsActive("Y");
        positive.setSysUserId(TEST_SYS_USER_ID);
        positiveValue = dictionaries.insert(positive);
        TestResult option = new TestResult();
        option.setTest(test);
        option.setTestResultType("D");
        option.setValue(positiveValue);
        option.setIsActive(true);
        option.setSysUserId(TEST_SYS_USER_ID);
        resultOptionId = options.insert(option);

        Localization localization = new Localization();
        localization.setDescription("test section name");
        localization.setLocalizedValue("en", "Mapping lifecycle");
        localization.setSysUserId(TEST_SYS_USER_ID);
        localizations.insert(localization);
        TestSection unit = new TestSection();
        unit.setLocalization(localization);
        unit.setTestSectionName("Mapping lifecycle");
        unit.setDescription("Lab unit owned by the mapping lifecycle test");
        unit.setIsActive("Y");
        unit.setSysUserId(TEST_SYS_USER_ID);
        labUnitId = labUnits.insert(unit);
        analyzer = createAnalyzer();
        originalRevisionId = analyzers.get(analyzer.analyzerId()).getSiteBindingRevision().getId();
        reload();
    }

    @Test
    public void unknownCodeAndValueCanBeSavedReopenedAndConfirmedWithHistory() throws Exception {
        assertEquals(1, imports.importBundle(unknownBundle(), TEST_SYS_USER_ID).resultsHeld());
        reload();
        AnalyzerTypeMappingView observed = mappings.getMapping(PROFILE_ID, PROFILE_REVISION);
        assertTrue("Received unknown code must be offered for mapping",
                observed.tests().stream().anyMatch(row -> SOURCE_CODE.equals(row.sourceRowKey())));

        AnalyzerTypeMappingView saved = mappings.saveMapping(PROFILE_ID, PROFILE_REVISION,
                new AnalyzerTypeMappingUpdate(observed.bindingFingerprint(),
                        List.of(new AnalyzerSiteBindingTestDraft(SOURCE_CODE, AnalyzerSiteBindingMappingState.BOUND,
                                testId)),
                        List.of(new AnalyzerSiteBindingResultDraft(SOURCE_CODE, RAW_VALUE,
                                AnalyzerSiteBindingMappingState.BOUND, resultOptionId))),
                TEST_SYS_USER_ID);
        reload();
        AnalyzerTypeMappingView reopened = mappings.getMapping(PROFILE_ID, PROFILE_REVISION);
        assertEquals(saved.bindingFingerprint(), reopened.bindingFingerprint());
        assertEquals(testId, reopened.tests().get(0).selectedTest().id());
        assertEquals(resultOptionId, reopened.tests().get(0).results().get(0).selectedOption().id());

        AnalyzerSiteBindingConfirmationRequest request = new AnalyzerSiteBindingConfirmationRequest(
                reopened.bindingFingerprint(), reopened.controlRecognition().recognitionFingerprint(),
                List.of(new AnalyzerSiteBindingSourceRow(SOURCE_CODE, null),
                        new AnalyzerSiteBindingSourceRow(SOURCE_CODE, RAW_VALUE)),
                List.of());
        assertEquals(AnalyzerSiteBindingConfirmationView.State.CURRENT,
                mappings.confirmMapping(PROFILE_ID, PROFILE_REVISION, request, TEST_SYS_USER_ID).state());
        reload();
        AnalyzerSiteBindingSnapshot current = bindings
                .findCurrentByProfileBindingId(analyzers.get(analyzer.analyzerId()).getPinnedProfileBinding().getId())
                .orElseThrow();
        var confirmation = confirmations.findByRevisionId(current.revision().getId()).orElseThrow();
        assertNotNull(confirmation.getAuditEventId());
        assertEquals(TEST_SYS_USER_ID, history.get(confirmation.getAuditEventId()).getSysUserId());
        assertEquals(confirmation.getId(), history.get(confirmation.getAuditEventId()).getReferenceId());
        assertTrue("The analyzer's old revision must remain unchanged",
                bindings.findByRevisionId(originalRevisionId).orElseThrow().tests().isEmpty());
        assertEquals("Confirming a shared mapping must not silently adopt it", originalRevisionId,
                analyzers.get(analyzer.analyzerId()).getSiteBindingRevision().getId());
    }

    @Test
    public void adoptionBeforeConfirmationCannotReleaseIncomingResults() throws Exception {
        AnalyzerTypeMappingView saved = saveReceivedMapping();
        adopt(analyzer, saved);
        assertTrue("An adopted but unconfirmed mapping must still hold incoming results",
                receive(analyzer).isReadOnly());
        confirm(saved);
        AnalyzerResults eligible = receive(analyzer);
        assertEquals(false, eligible.isReadOnly());
        assertEquals(testId, eligible.getTestId());
        assertEquals(positiveValue, eligible.getResult());
    }

    @Test
    public void adoptingANewRevisionRecordsTheActorAndPreviousSelectionOnce() throws Exception {
        AnalyzerTypeMappingView saved = saveReceivedMapping();
        var referenceTable = referenceTables.getReferenceTableByName("analyzer");
        assertEquals("Analyzer changes must be enabled for auditing", "Y", referenceTable.getKeepHistory());
        String tableId = referenceTable.getId();
        var before = history.getHistoryByRefIdAndRefTableId(analyzer.analyzerId(), tableId);
        var previousHistoryIds = before.stream().map(row -> row.getId()).toList();

        adopt(analyzer, saved);
        var newHistory = history.getHistoryByRefIdAndRefTableId(analyzer.analyzerId(), tableId).stream()
                .filter(row -> !previousHistoryIds.contains(row.getId())).toList();
        assertEquals("Adoption must persist a change-history entry", 1, newHistory.size());
        var entry = newHistory.get(0);
        assertEquals(TEST_SYS_USER_ID, entry.getSysUserId());
        assertEquals("U", entry.getActivity());
        assertNotNull(entry.getTimestamp());
        String changes = new String(entry.getChanges(), StandardCharsets.UTF_8);
        assertTrue(changes, changes.contains("siteBindingRevision"));
        assertTrue(changes, changes.contains("<siteBindingRevision>" + originalRevisionId + "</siteBindingRevision>"));
        assertEquals(saved.siteBindingRevision(),
                analyzers.get(analyzer.analyzerId()).getSiteBindingRevision().getRevisionNumber());

        adopt(analyzer, saved);
        assertEquals("Repeated adoption must not invent another change", before.size() + 1,
                history.getHistoryByRefIdAndRefTableId(analyzer.analyzerId(), tableId).size());
    }

    @Test
    public void confirmationRequiresEachAnalyzerToAdoptItsOwnRevision() throws Exception {
        AnalyzerInstanceState second = createAnalyzer();
        AnalyzerTypeMappingView saved = saveReceivedMapping();
        confirm(saved);
        assertTrue("Confirmation alone must not change the first analyzer's mapping", receive(analyzer).isReadOnly());
        assertTrue("Confirmation alone must not change the second analyzer's mapping", receive(second).isReadOnly());
        adopt(analyzer, saved);
        assertEquals(false, receive(analyzer).isReadOnly());
        assertTrue("The second analyzer must remain on its selected old revision", receive(second).isReadOnly());
        adopt(second, saved);
        assertEquals(false, receive(second).isReadOnly());
    }

    @Test
    public void aNewUnconfirmedRevisionCannotReplaceAnAnalyzersConfirmedSelection() throws Exception {
        AnalyzerTypeMappingView selected = saveReceivedMapping();
        confirm(selected);
        adopt(analyzer, selected);
        AnalyzerTypeMappingView later = mappings.saveMapping(PROFILE_ID, PROFILE_REVISION,
                new AnalyzerTypeMappingUpdate(selected.bindingFingerprint(),
                        List.of(new AnalyzerSiteBindingTestDraft(SOURCE_CODE, AnalyzerSiteBindingMappingState.EXCLUDED,
                                null)),
                        List.of(new AnalyzerSiteBindingResultDraft(SOURCE_CODE, RAW_VALUE,
                                AnalyzerSiteBindingMappingState.EXCLUDED, null))),
                TEST_SYS_USER_ID);
        assertTrue(later.siteBindingRevision() > selected.siteBindingRevision());
        AnalyzerResults incoming = receive(analyzer);
        assertEquals(false, incoming.isReadOnly());
        assertEquals("The confirmed selected revision still maps the original value", positiveValue,
                incoming.getResult());
    }

    @Test
    public void confirmationCanCoverResolvedRowsWhileUnresolvedRowsRemainHeld() throws Exception {
        receive(analyzer);
        Bundle additional = unknownBundle(analyzer, "UNRESOLVED-INITIAL");
        additional.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast)
                .forEach(observation -> observation.getCode().getCodingFirstRep().setCode("STILL-UNKNOWN"));
        imports.importBundle(additional, TEST_SYS_USER_ID);
        AnalyzerTypeMappingView observed = mappings.getMapping(PROFILE_ID, PROFILE_REVISION);
        AnalyzerTypeMappingView partial = mappings.saveMapping(PROFILE_ID, PROFILE_REVISION,
                new AnalyzerTypeMappingUpdate(observed.bindingFingerprint(), List.of(
                        new AnalyzerSiteBindingTestDraft(SOURCE_CODE, AnalyzerSiteBindingMappingState.BOUND, testId),
                        new AnalyzerSiteBindingTestDraft("STILL-UNKNOWN", AnalyzerSiteBindingMappingState.UNRESOLVED,
                                null)),
                        List.of(new AnalyzerSiteBindingResultDraft(SOURCE_CODE, RAW_VALUE,
                                AnalyzerSiteBindingMappingState.BOUND, resultOptionId))),
                TEST_SYS_USER_ID);
        confirm(partial);
        adopt(analyzer, partial);
        assertEquals(false, receive(analyzer).isReadOnly());
        additional.getIdentifier().setValue(UUID.randomUUID().toString());
        additional.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Specimen.class::isInstance)
                .map(Specimen.class::cast)
                .forEach(specimen -> specimen.getIdentifierFirstRep().setValue("UNRESOLVED-LATER"));
        assertEquals(1, imports.importBundle(additional, TEST_SYS_USER_ID).resultsHeld());
    }

    private AnalyzerTypeMappingView saveReceivedMapping() throws Exception {
        receive(analyzer);
        AnalyzerTypeMappingView observed = mappings.getMapping(PROFILE_ID, PROFILE_REVISION);
        return mappings.saveMapping(PROFILE_ID, PROFILE_REVISION,
                new AnalyzerTypeMappingUpdate(observed.bindingFingerprint(),
                        List.of(new AnalyzerSiteBindingTestDraft(SOURCE_CODE, AnalyzerSiteBindingMappingState.BOUND,
                                testId)),
                        List.of(new AnalyzerSiteBindingResultDraft(SOURCE_CODE, RAW_VALUE,
                                AnalyzerSiteBindingMappingState.BOUND, resultOptionId))),
                TEST_SYS_USER_ID);
    }

    private void confirm(AnalyzerTypeMappingView view) {
        mappings.confirmMapping(PROFILE_ID, PROFILE_REVISION,
                new AnalyzerSiteBindingConfirmationRequest(view.bindingFingerprint(),
                        view.controlRecognition().recognitionFingerprint(),
                        List.of(new AnalyzerSiteBindingSourceRow(SOURCE_CODE, null),
                                new AnalyzerSiteBindingSourceRow(SOURCE_CODE, RAW_VALUE)),
                        List.of()),
                TEST_SYS_USER_ID);
        reload();
    }

    private void adopt(AnalyzerInstanceState instance, AnalyzerTypeMappingView view) {
        instances.selectSiteBindingRevision(instance.analyzerId(), view.siteBindingId(), view.siteBindingRevision(),
                view.bindingFingerprint(), TEST_SYS_USER_ID);
        reload();
    }

    private AnalyzerResults receive(AnalyzerInstanceState instance) throws Exception {
        String accession = "LIFE-" + UUID.randomUUID().toString().substring(0, 12);
        imports.importBundle(unknownBundle(instance, accession), TEST_SYS_USER_ID);
        reload();
        return results.getResultsbyAnalyzer(instance.analyzerId()).stream()
                .filter(row -> accession.equals(row.getAccessionNumber())).findFirst().orElseThrow();
    }

    private AnalyzerInstanceState createAnalyzer() {
        AnalyzerInstanceRequest request = new AnalyzerInstanceRequest();
        request.setTestUnitIds(List.of(labUnitId));
        request.setName("Lifecycle analyzer " + UUID.randomUUID());
        request.setProfileId(PROFILE_ID);
        request.setProfileRevision(PROFILE_REVISION);
        AnalyzerInstanceState instance = instances.create(request, TEST_SYS_USER_ID);
        return instances.attachBridgeConnection(instance.analyzerId(), "lifecycle-" + UUID.randomUUID(),
                TEST_SYS_USER_ID);
    }

    private Bundle unknownBundle() throws Exception {
        return unknownBundle(analyzer, "ACC-UNKNOWN-TEST-001");
    }

    private Bundle unknownBundle(AnalyzerInstanceState instance, String accession) throws Exception {
        Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class,
                Files.readString(Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1", "fixtures",
                        "normalized-unknown-test.fhir.json")));
        bundle.getIdentifier().setValue(UUID.randomUUID().toString());
        Device device = (Device) bundle.getEntry().get(0).getResource();
        device.getIdentifier().stream().filter(id -> id.getSystem().endsWith("analyzer-connection-id"))
                .forEach(id -> id.setValue(instance.bridgeConnectionId()));
        device.getIdentifier().stream().filter(id -> id.getSystem().endsWith("/analyzer-id"))
                .forEach(id -> id.setValue(instance.analyzerId()));
        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Specimen.class::isInstance)
                .map(Specimen.class::cast).forEach(specimen -> specimen.getIdentifierFirstRep().setValue(accession));
        device.getExtensionByUrl(EXTENSION + "profile-id").setValue(new StringType(PROFILE_ID));
        device.getExtensionByUrl(EXTENSION + "profile-revision").setValue(new IntegerType(PROFILE_REVISION));
        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Observation.class::isInstance)
                .map(Observation.class::cast).forEach(observation -> {
                    // Match the Bridge's NONE-mode evidence for this external catalog fixture.
                    Extension recognition = observation.getExtensionByUrl(EXTENSION + "control-recognition");
                    recognition.getExtension().clear();
                    recognition.addExtension("mode", new CodeType("NONE"));
                    recognition.addExtension("outcome", new CodeType("NOT_EVALUATED"));
                    recognition.addExtension("recognitionFingerprint", new StringType(RECOGNITION_FINGERPRINT));
                });
        return bundle;
    }

    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }
}
