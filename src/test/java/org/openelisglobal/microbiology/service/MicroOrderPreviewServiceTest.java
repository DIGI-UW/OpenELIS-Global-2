package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.microbiology.form.MicroOrderPreviewRequestForm;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MicroOrderPreviewServiceTest {
    private TestService tests;
    private TypeOfSampleService types;
    private TestReflexService reflex;
    private MicroOrderPreviewService service;
    private RuleResultScope scope;
    private DictionaryService dictionary;
    private TestResultComponentService components;

    @Before
    public void setUp() {
        tests = mock(TestService.class);
        types = mock(TypeOfSampleService.class);
        reflex = mock(TestReflexService.class);
        UserService users = mock(UserService.class);
        RoleService roles = mock(RoleService.class);
        Role reception = new Role();
        reception.setId("10");
        when(roles.getRoleByName(Constants.ROLE_RECEPTION)).thenReturn(reception);
        when(users.getUserTestSections("user", "10"))
                .thenReturn(List.of(new IdValuePair("1", "Microbiology"), new IdValuePair("2", "TB")));
        TypeOfSample type = mock(TypeOfSample.class);
        when(type.getId()).thenReturn("5");
        when(type.getLocalizedName()).thenReturn("Sputum");
        when(types.get("5")).thenReturn(type);
        scope = mock(RuleResultScope.class);
        dictionary = mock(DictionaryService.class);
        components = mock(TestResultComponentService.class);
        when(scope.resultTypeForComponent(anyString(), any(), any())).thenReturn("D");
        Dictionary positive = mock(Dictionary.class);
        when(positive.getLocalizedName()).thenReturn("Positive");
        when(dictionary.getDictionaryById("901")).thenReturn(positive);
        MicroOrderRoutingService routing = mock(MicroOrderRoutingService.class);
        when(routing.previewNewOrder(anyList())).thenAnswer(call -> MicroOrderDraftGrouping.group(call.getArgument(0)));
        service = new MicroOrderPreviewServiceImpl(routing, tests, types, users, roles, reflex, scope, dictionary,
                components, new MicroCultureSetWarningService(30),
                mock(org.openelisglobal.dictionary.service.SampleContainerClassificationService.class));
    }

    @Test
    public void describesCaseSplitsOrdinaryResultsAndNamedActiveReflexWithoutWrites() {
        catalog("culture", "Culture", "1", true);
        catalog("tb", "TB culture", "2", true);
        catalog("rpr", "RPR", "1", false);
        catalog("gram", "Gram stain", "1", false);
        ReflexRule rule = new ReflexRule();
        rule.setRuleName("Positive culture follow-up");
        rule.setOverall(ReflexRuleOptions.OverallOptions.ANY);
        ReflexRuleCondition condition = new ReflexRuleCondition();
        condition.setTestId("culture");
        condition.setRelation(ReflexRuleOptions.NumericRelationOptions.EQUALS);
        condition.setValue("901");
        rule.setConditions(Set.of(condition));
        ReflexRuleAction action = new ReflexRuleAction();
        action.setReflexTestId("gram");
        rule.setActions(Set.of(action));
        when(reflex.getAllReflexRules()).thenReturn(List.of(rule));

        var preview = service.preview(request("culture", "tb", "rpr"), "user");
        assertEquals(2, preview.cases().size());
        assertEquals(1, preview.newUnitWarnings().size());
        assertEquals("2", preview.newUnitWarnings().get(0).labUnitId());
        assertEquals("TB culture", preview.newUnitWarnings().get(0).testName());
        assertEquals(List.of("Culture"), preview.cases().get(0).testNames());
        assertEquals("RPR", preview.ordinaryTests().get(0).testName());
        assertEquals(List.of("Microbiology", "TB"), preview.warnings().get(0).labUnits());
        assertEquals("Positive culture follow-up", preview.reflexRules().get(0).name());
        assertEquals(List.of("Gram stain"), preview.reflexRules().get(0).addedTests());
        assertEquals("Positive", preview.reflexRules().get(0).conditions().get(0).value());
        assertEquals("EQUALS", preview.reflexRules().get(0).conditions().get(0).relation());
        assertEquals("ANY", preview.reflexRules().get(0).overall());
        rule.setActive(false);
        assertTrue(service.preview(request("culture"), "user").reflexRules().isEmpty());
    }

    @Test
    public void numericComponentKeepsBothBoundsInsteadOfLookingUpDictionaryIds() {
        catalog("culture", "Culture", "1", true);
        catalog("repeat", "Repeat culture", "1", true);
        TestResultComponent component = new TestResultComponent();
        component.setId("component-2");
        component.setTestId("culture");
        component.setLabel("Colony count");
        when(components.get("component-2")).thenReturn(component);
        when(scope.resultTypeForComponent(eq("culture"), eq("component-2"), any())).thenReturn("N");
        ReflexRuleCondition condition = new ReflexRuleCondition();
        condition.setTestId("culture");
        condition.setComponentId("component-2");
        condition.setSampleId("5");
        condition.setRelation(ReflexRuleOptions.NumericRelationOptions.BETWEEN);
        condition.setValue("10");
        condition.setValue2("20");
        ReflexRuleAction action = new ReflexRuleAction();
        action.setReflexTestId("repeat");
        ReflexRule rule = new ReflexRule();
        rule.setRuleName("Repeat borderline count");
        rule.setOverall(ReflexRuleOptions.OverallOptions.ALL);
        rule.setConditions(Set.of(condition));
        rule.setActions(Set.of(action));
        when(reflex.getAllReflexRules()).thenReturn(List.of(rule));

        var line = service.preview(request("culture"), "user").reflexRules().get(0);
        assertEquals("ALL", line.overall());
        assertEquals("Colony count", line.conditions().get(0).componentLabel());
        assertEquals("Sputum", line.conditions().get(0).sampleTypeName());
        assertEquals("10", line.conditions().get(0).value());
        assertEquals("20", line.conditions().get(0).value2());
        verifyZeroInteractions(dictionary);

        condition.setSampleId("other-type");
        assertTrue(service.preview(request("culture"), "user").reflexRules().isEmpty());
    }

    @Test
    public void refusesTestsOutsideReceptionUnits() {
        catalog("denied", "Restricted culture", "3", true);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.preview(request("denied"), "user"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyZeroInteractions(reflex);
    }

    @Test
    public void newUnitWarningDoesNotBlockAndOtherWorkInTheUnitRemovesIt() {
        catalog("culture", "Culture", "1", true);
        catalog("rpr", "RPR", "1", false);
        var first = service.preview(request("culture"), "user");
        assertEquals(1, first.cases().size());
        assertEquals(1, first.newUnitWarnings().size());
        assertTrue(service.preview(request("culture", "rpr"), "user").newUnitWarnings().isEmpty());
        var multipleSpecimens = request("culture");
        multipleSpecimens.specimens.add(request("culture").specimens.get(0));
        var repeated = service.preview(multipleSpecimens, "user");
        assertEquals(1, repeated.cases().size());
        assertTrue(repeated.newUnitWarnings().isEmpty());
    }

    @Test
    public void requiresAnAuthenticatedActor() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.preview(request(), ""));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyZeroInteractions(tests, types, reflex);
    }

    @Test
    public void ordinaryOnlySelectionDoesNotPredictACase() {
        catalog("rpr", "RPR", "1", false);
        var preview = service.preview(request("rpr"), "user");
        assertTrue(preview.cases().isEmpty());
        assertEquals(1, preview.ordinaryTests().size());
        verifyZeroInteractions(reflex);
    }

    @Test
    public void previewUsesExplicitSetAssignmentsAndSameWarningRulesWithoutWrites() {
        catalog("culture", "Blood culture", "1", true);
        when(tests.get("culture").isCollectedInSets()).thenReturn(true);
        when(tests.get("culture").getMicrobiologyCaseRole()).thenReturn("CULTURE");
        var request = request("culture");
        request.specimens.get(0).cultureSetNumber = 1;
        var second = new MicroOrderPreviewRequestForm.Specimen();
        second.sampleTypeId = "5";
        second.testIds = List.of("culture");
        second.cultureSetNumber = 2;
        request.specimens = List.of(request.specimens.get(0), second);
        var preview = service.preview(request, "user");
        assertEquals(1, preview.cases().size());
        assertEquals(2, preview.cases().get(0).bottles().size());
        assertEquals(List.of(1, 2), preview.cases().get(0).setWarnings().stream().map(w -> w.setNumber()).toList());
        second.cultureSetNumber = 1;
        assertTrue(service.preview(request, "user").cases().get(0).setWarnings().isEmpty());
        var first = request.specimens.get(0);
        first.container = "Aerobic";
        second.container = "Aerobic";
        first.bodySite = "Left arm";
        second.bodySite = "Right arm";
        first.collectionDate = second.collectionDate = "2026-10-07";
        first.collectionTime = "07:00";
        second.collectionTime = "07:31";
        assertEquals(List.of("REPEATED_CONTAINER", "DIFFERENT_SITES", "COLLECTION_INTERVAL"),
                service.preview(request, "user").cases().get(0).setWarnings().stream().map(w -> w.code()).toList());
    }

    private MicroOrderPreviewRequestForm request(String... ids) {
        var request = new MicroOrderPreviewRequestForm();
        var specimen = new MicroOrderPreviewRequestForm.Specimen();
        specimen.sampleTypeId = "5";
        specimen.testIds = List.of(ids);
        request.specimens.add(specimen);
        return request;
    }

    @Test(expected = IllegalArgumentException.class)
    public void inactiveMicroCatalogWorkCannotBePromisedByThePreview() {
        catalog("culture", "Culture", "1", true);
        when(tests.get("culture").isActive()).thenReturn(false);
        service.preview(request("culture"), "user");
    }

    private void catalog(String id, String name, String unitId, boolean micro) {
        var test = mock(org.openelisglobal.test.valueholder.Test.class);
        when(test.getId()).thenReturn(id);
        when(test.isActive()).thenReturn(true);
        when(test.getMicrobiologyCaseRole()).thenReturn("DIRECT");
        when(test.getLocalizedName()).thenReturn(name);
        when(test.isOpensMicrobiologyCase()).thenReturn(micro);
        TestSection unit = new TestSection();
        unit.setId(unitId);
        unit.setIsActive("Y");
        unit.setTestSectionName("1".equals(unitId) ? "Microbiology" : "TB");
        when(test.getTestSection()).thenReturn(unit);
        when(tests.get(id)).thenReturn(test);
    }
}
