package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.microbiology.form.MicroOrderPreviewRequestForm;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MicroOrderPreviewServiceTest {
    private TestService tests;
    private TypeOfSampleService types;
    private TestReflexService reflex;
    private MicroCaseService cases;
    private MicroCaseAnalysisService links;
    private MicroOrderPreviewService service;

    @Before
    public void setUp() {
        tests = mock(TestService.class);
        types = mock(TypeOfSampleService.class);
        reflex = mock(TestReflexService.class);
        cases = mock(MicroCaseService.class);
        links = mock(MicroCaseAnalysisService.class);
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
        service = new MicroOrderPreviewServiceImpl(new MicroOrderRoutingServiceImpl(cases, links, tests), tests, types,
                users, roles, reflex);
    }

    @Test
    public void describesCaseSplitsOrdinaryResultsAndNamedActiveReflexWithoutWrites() {
        catalog("culture", "Culture", "1", true);
        catalog("tb", "TB culture", "2", true);
        catalog("rpr", "RPR", "1", false);
        catalog("gram", "Gram stain", "1", false);
        ReflexRule rule = new ReflexRule();
        rule.setRuleName("Positive culture follow-up");
        ReflexRuleCondition condition = new ReflexRuleCondition();
        condition.setTestId("culture");
        rule.setConditions(Set.of(condition));
        ReflexRuleAction action = new ReflexRuleAction();
        action.setReflexTestId("gram");
        rule.setActions(Set.of(action));
        when(reflex.getAllReflexRules()).thenReturn(List.of(rule));

        var preview = service.preview(request("culture", "tb", "rpr"), "user");
        assertEquals(2, preview.cases().size());
        assertEquals(List.of("Culture"), preview.cases().get(0).testNames());
        assertEquals("RPR", preview.ordinaryTests().get(0).testName());
        assertEquals(List.of("Microbiology", "TB"), preview.warnings().get(0).labUnits());
        assertEquals("Positive culture follow-up", preview.reflexRules().get(0).name());
        assertEquals(List.of("Gram stain"), preview.reflexRules().get(0).addedTests());
        rule.setActive(false);
        assertTrue(service.preview(request("culture"), "user").reflexRules().isEmpty());
        verifyZeroInteractions(cases, links);
    }

    @Test
    public void refusesTestsOutsideReceptionUnits() {
        catalog("denied", "Restricted culture", "3", true);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.preview(request("denied"), "user"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyZeroInteractions(cases, links, reflex);
    }

    @Test
    public void requiresAnAuthenticatedActor() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.preview(request(), ""));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyZeroInteractions(tests, types, cases, links, reflex);
    }

    @Test
    public void ordinaryOnlySelectionDoesNotPredictACase() {
        catalog("rpr", "RPR", "1", false);
        var preview = service.preview(request("rpr"), "user");
        assertTrue(preview.cases().isEmpty());
        assertEquals(1, preview.ordinaryTests().size());
        verifyZeroInteractions(cases, links, reflex);
    }

    private MicroOrderPreviewRequestForm request(String... ids) {
        var request = new MicroOrderPreviewRequestForm();
        var specimen = new MicroOrderPreviewRequestForm.Specimen();
        specimen.sampleTypeId = "5";
        specimen.testIds = List.of(ids);
        request.specimens.add(specimen);
        return request;
    }

    private void catalog(String id, String name, String unitId, boolean micro) {
        var test = mock(org.openelisglobal.test.valueholder.Test.class);
        when(test.getId()).thenReturn(id);
        when(test.getLocalizedName()).thenReturn(name);
        when(test.isOpensMicrobiologyCase()).thenReturn(micro);
        TestSection unit = new TestSection();
        unit.setId(unitId);
        unit.setTestSectionName("1".equals(unitId) ? "Microbiology" : "TB");
        when(test.getTestSection()).thenReturn(unit);
        when(tests.get(id)).thenReturn(test);
    }
}
