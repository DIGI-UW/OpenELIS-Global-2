package org.openelisglobal.validationpolicy.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.service.ValidationPolicyService;
import org.openelisglobal.validationpolicy.service.ValidationPolicyStaleException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1017 review: a policy save that passed the version check but lost to a
 * save committed in the meantime is refused like a save from an outdated page,
 * with the stored policy's id and version, so the page can name who changed it.
 */
public class ValidationPolicyLostRaceTest {

    private static final Timestamp STORED = new Timestamp(1_700_000_000_000L);

    private ValidationPolicyService service;
    private ValidationPolicyRestController controller;
    private MockHttpServletRequest request;

    @Before
    public void controller() {
        service = mock(ValidationPolicyService.class);
        controller = new ValidationPolicyRestController();
        ReflectionTestUtils.setField(controller, "validationPolicyService", service);
        request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(1);
        request.setAttribute(IActionConstants.USER_SESSION_DATA, user);
    }

    @Test
    public void aDefaultSaveThatLostTheRaceNamesTheStoredDefault() {
        when(service.saveLabWideDefault(any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("ValidationPolicy", 3L));
        when(service.lostRace(null)).thenReturn(new ValidationPolicyStaleException(3L, STORED));

        ValidationPolicyStaleException refused = assertThrows(ValidationPolicyStaleException.class,
                () -> controller.saveLabWideDefault(request, new ValidationPolicyForm("ALL", 1, null, "1")));

        assertEquals(Long.valueOf(3L), refused.getPolicyId());
        assertEquals(STORED, refused.getCurrent());
    }

    @Test
    public void anOverrideSaveThatLostTheRaceNamesTheStoredOverride() {
        when(service.saveLabUnitOverride(eq(7L), any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("ValidationPolicy", 7L));
        when(service.lostRace(7L)).thenReturn(new ValidationPolicyStaleException(7L, STORED));

        ValidationPolicyStaleException refused = assertThrows(ValidationPolicyStaleException.class,
                () -> controller.saveLabUnitOverride(request, 7L, new ValidationPolicyForm("ALL", 1, null, "1")));

        assertEquals(Long.valueOf(7L), refused.getPolicyId());
        assertEquals(STORED, refused.getCurrent());
    }
}
