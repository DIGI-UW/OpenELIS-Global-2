package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.form.SampleEditForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1514: the tests Modify Order offers for an existing sample are the sample
 * type's active orderable tests that are not already live on the sample item,
 * and each carries the sample type it belongs to so the screen can offer the
 * type's panels.
 */
public class SampleEditAvailableTestsTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "AVAIL-0001";

    @Autowired
    private IStatusService statusService;

    private SampleEditRestController controller;
    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/sample-edit-available-tests.xml");
        authenticateAs("testUser");
        statusService.refreshCache();
        controller = webApplicationContext.getAutowireCapableBeanFactory().createBean(SampleEditRestController.class);
        pinStatusIdsToThisFixture();
        request = new MockHttpServletRequest();
        request.setSession(buildSession());
    }

    @SuppressWarnings("unchecked")
    private void pinStatusIdsToThisFixture() {
        Set<String> entered = (Set<String>) ReflectionTestUtils.getField(SampleEditRestController.class,
                "ENTERED_STATUS_SAMPLE_LIST");
        entered.clear();
        entered.add(statusService.getStatusID(SampleStatus.Entered));
        Set<String> excluded = (Set<String>) ReflectionTestUtils.getField(SampleEditRestController.class,
                "excludedAnalysisStatusList");
        excluded.clear();
        excluded.add(statusService.getStatusID(AnalysisStatus.Canceled));
    }

    private MockHttpSession buildSession() {
        UserDetails userDetails = User.withUsername("testUser").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return httpSession;
    }

    private List<SampleEditItem> availableTests() throws Exception {
        SampleEditForm form = controller.showSampleEdit(request, ACCESSION, null);
        return form.getPossibleTests();
    }

    @Test
    public void availableTests_leaveOutTheTestsAlreadyLiveOnTheSample() throws Exception {
        List<String> names = availableTests().stream().map(SampleEditItem::getTestName).collect(Collectors.toList());

        assertEquals("Lead is live on the item, Nickel is inactive; the cancelled Copper can be re-added",
                List.of("Copper (Cu)", "Zinc (Zn)"), names);
    }

    @Test
    public void availableTests_carryTheirSampleItemAndItsSampleType() throws Exception {
        List<SampleEditItem> tests = availableTests();

        for (SampleEditItem test : tests) {
            assertEquals("9201", test.getSampleItemId());
            assertEquals("1", test.getSampleTypeId());
        }
        assertEquals(ACCESSION + "-1", tests.get(0).getAccessionNumber());
        assertEquals("Water Sample", tests.get(0).getSampleType());
    }
}
