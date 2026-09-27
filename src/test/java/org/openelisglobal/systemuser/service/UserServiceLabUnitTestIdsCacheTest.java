package org.openelisglobal.systemuser.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.test.service.TestService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Working out which tests a user may touch reads every test in their lab units,
 * and several screens ask for it many times while serving one request: a
 * patient report walks the patient's samples, a STAT notification walks every
 * holder of a role. Each answer is therefore worked out once per request.
 *
 * <p>
 * Before that, a patient with a few hundred samples read the whole catalogue
 * once per sample and their report took minutes, past the point where the
 * browser gives up.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceLabUnitTestIdsCacheTest {

    private static final String READER = "7";
    private static final String COLLEAGUE = "8";
    private static final String ROLE_ID = "5";
    private static final List<String> HEMATOLOGY = Arrays.asList("36");

    @Mock
    private RoleService roleService;

    @Mock
    private TestService testService;

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    @Before
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        Role role = new Role();
        role.setId(ROLE_ID);
        when(roleService.getRoleByName(Constants.ROLE_RESULTS)).thenReturn(role);
        when(testService.getTestsByTestSectionIds(anyList())).thenReturn(tests("11", "12"));
        doReturn(Arrays.asList(new IdValuePair("36", "Hematology"))).when(userService).getUserTestSections(READER,
                ROLE_ID);
        doReturn(Arrays.asList(new IdValuePair("36", "Hematology"))).when(userService).getUserTestSections(COLLEAGUE,
                ROLE_ID);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testIdsInUserLabUnits_readTheCatalogueOncePerRequest() {
        Set<String> first = userService.getTestIdsInUserLabUnits(READER, Constants.ROLE_RESULTS);
        Set<String> second = userService.getTestIdsInUserLabUnits(READER, Constants.ROLE_RESULTS);

        assertEquals("the same tests come back every time", new LinkedHashSet<>(Arrays.asList("11", "12")), first);
        assertEquals(first, second);
        verify(testService, times(1)).getTestsByTestSectionIds(HEMATOLOGY);
    }

    @Test
    public void testIdsInUserLabUnits_shareOneReadBetweenUsersOfTheSameLabUnits() {
        userService.getTestIdsInUserLabUnits(READER, Constants.ROLE_RESULTS);
        userService.getTestIdsInUserLabUnits(COLLEAGUE, Constants.ROLE_RESULTS);

        verify(testService, times(1)).getTestsByTestSectionIds(HEMATOLOGY);
    }

    @Test
    public void testIdsInUserLabUnits_answerTheSameOutsideARequest() {
        RequestContextHolder.resetRequestAttributes();

        assertEquals("a scheduled or daemon caller still gets its answer",
                new LinkedHashSet<>(Arrays.asList("11", "12")),
                userService.getTestIdsInUserLabUnits(READER, Constants.ROLE_RESULTS));
    }

    @Test
    public void testIdsInUserLabUnits_areEmptyForAUserWithNoLabUnits() {
        doReturn(Arrays.asList()).when(userService).getUserTestSections(READER, ROLE_ID);

        assertEquals("no lab unit means no test", 0,
                userService.getTestIdsInUserLabUnits(READER, Constants.ROLE_RESULTS).size());
        verify(testService, times(0)).getTestsByTestSectionIds(anyList());
    }

    private List<org.openelisglobal.test.valueholder.Test> tests(String... ids) {
        return Arrays.stream(ids).map(id -> {
            org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
            test.setId(id);
            return test;
        }).collect(Collectors.toList());
    }
}
