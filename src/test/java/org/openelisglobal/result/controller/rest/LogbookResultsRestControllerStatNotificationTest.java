package org.openelisglobal.result.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisAnchorService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.referral.service.ReferralTypeService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression test for Issue #4862: STAT Result notifications must not
 * accumulate test names across users in loop.
 */
@RunWith(MockitoJUnitRunner.class)
public class LogbookResultsRestControllerStatNotificationTest {

    @Mock
    private ReferralTypeService referralTypeService;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private UserService userService;
    @Mock
    private AnalysisAnchorService analysisAnchorService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private NotificationDAO notificationDAO;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private LogbookResultsRestController controller;

    @Before
    public void setUp() {
        MessageUtil.setMessageSource(messageSource);
        ReflectionTestUtils.setField(controller, "userRoleService", userRoleService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "analysisAnchorService", analysisAnchorService);
        ReflectionTestUtils.setField(controller, "systemUserService", systemUserService);
        ReflectionTestUtils.setField(controller, "notificationDAO", notificationDAO);
    }

    @Test
    public void sendStatResultNotifications_doesNotAccumulateTestsAcrossUsers() {
        Analysis analysis1 = createMockAnalysis("Full Blood Count");
        Analysis analysis2 = createMockAnalysis("Liver Function Test");
        List<Analysis> newResultAnalyses = Arrays.asList(analysis1, analysis2);

        Sample statSample1 = mock(Sample.class);
        when(statSample1.getPriority()).thenReturn(OrderPriority.STAT);
        when(statSample1.getAccessionNumber()).thenReturn("LAB-101");
        when(analysisAnchorService.resolveSample(analysis1)).thenReturn(statSample1);

        Sample statSample2 = mock(Sample.class);
        when(statSample2.getPriority()).thenReturn(OrderPriority.STAT);
        when(statSample2.getAccessionNumber()).thenReturn("LAB-102");
        when(analysisAnchorService.resolveSample(analysis2)).thenReturn(statSample2);

        when(userRoleService.getUserIdsForRole(Constants.ROLE_VALIDATION)).thenReturn(Arrays.asList("user1", "user2"));

        when(messageSource.getMessage(eq("notification.result.stat"), any(), any(), any()))
                .thenReturn("STAT priority Test Results have been created for Tests : ");

        when(userService.filterAnalysesByLabUnitRoles("user1", newResultAnalyses, Constants.ROLE_VALIDATION))
                .thenReturn(Collections.singletonList(analysis1));
        when(userService.filterAnalysesByLabUnitRoles("user2", newResultAnalyses, Constants.ROLE_VALIDATION))
                .thenReturn(Collections.singletonList(analysis2));

        SystemUser user1 = new SystemUser();
        user1.setId("user1");
        SystemUser user2 = new SystemUser();
        user2.setId("user2");
        when(systemUserService.getUserById("user1")).thenReturn(user1);
        when(systemUserService.getUserById("user2")).thenReturn(user2);

        controller.sendStatResultNotifications(newResultAnalyses);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDAO, times(2)).save(captor.capture());

        List<Notification> capturedNotifications = captor.getAllValues();
        String expectedAccession1 = AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay("LAB-101");
        String expectedAccession2 = AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay("LAB-102");

        assertEquals("user1", capturedNotifications.get(0).getUser().getId());
        assertEquals("STAT priority Test Results have been created for Tests : " + expectedAccession1
                + " - Full Blood Count", capturedNotifications.get(0).getMessage());

        assertEquals("user2", capturedNotifications.get(1).getUser().getId());
        assertEquals("STAT priority Test Results have been created for Tests : " + expectedAccession2
                + " - Liver Function Test", capturedNotifications.get(1).getMessage());
    }

    @Test
    public void sendStatResultNotifications_isNoOp_whenListEmptyOrNull() {
        controller.sendStatResultNotifications(null);
        controller.sendStatResultNotifications(Collections.emptyList());

        verify(notificationDAO, never()).save(any());
    }

    private Analysis createMockAnalysis(String testName) {
        Analysis analysis = mock(Analysis.class);
        org.openelisglobal.test.valueholder.Test testEntity = mock(org.openelisglobal.test.valueholder.Test.class);
        when(testEntity.getLocalizedName()).thenReturn(testName);
        when(analysis.getTest()).thenReturn(testEntity);
        return analysis;
    }
}
