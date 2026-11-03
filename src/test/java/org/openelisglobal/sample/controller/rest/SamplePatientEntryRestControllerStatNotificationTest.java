package org.openelisglobal.sample.controller.rest;

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
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.context.MessageSource;

/**
 * Regression test for Issue #4862: STAT Order notifications must not accumulate
 * test names across users in loop.
 */
@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryRestControllerStatNotificationTest {

    @Mock
    private UserRoleService userRoleService;
    @Mock
    private SampleService sampleService;
    @Mock
    private UserService userService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private NotificationDAO notificationDAO;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private SamplePatientEntryRestController controller;

    @Before
    public void setUp() {
        MessageUtil.setMessageSource(messageSource);
    }

    @Test
    public void sendStatOrderNotifications_doesNotAccumulateTestsAcrossUsers() {
        SampleOrderItem orderItem = new SampleOrderItem();
        orderItem.setLabNo("LAB-101");
        orderItem.setPriority(OrderPriority.STAT);

        when(userRoleService.getUserIdsForRole(Constants.ROLE_RESULTS)).thenReturn(Arrays.asList("user1", "user2"));

        Sample sample = mock(Sample.class);
        when(sampleService.getSampleByAccessionNumber("LAB-101")).thenReturn(sample);

        Analysis analysis1 = createMockAnalysis("Full Blood Count");
        Analysis analysis2 = createMockAnalysis("Liver Function Test");
        List<Analysis> allAnalyses = Arrays.asList(analysis1, analysis2);
        when(sampleService.getAnalysis(sample)).thenReturn(allAnalyses);

        when(messageSource.getMessage(eq("notification.order.stat"), any(), any(), any()))
                .thenReturn("STAT priority Order with Lab Number LAB-101 has been Created for Tests : ");

        when(userService.filterAnalysesByLabUnitRoles("user1", allAnalyses, Constants.ROLE_RESULTS))
                .thenReturn(Collections.singletonList(analysis1));
        when(userService.filterAnalysesByLabUnitRoles("user2", allAnalyses, Constants.ROLE_RESULTS))
                .thenReturn(Collections.singletonList(analysis2));

        SystemUser user1 = new SystemUser();
        user1.setId("user1");
        SystemUser user2 = new SystemUser();
        user2.setId("user2");
        when(systemUserService.getUserById("user1")).thenReturn(user1);
        when(systemUserService.getUserById("user2")).thenReturn(user2);

        controller.sendStatOrderNotifications(orderItem);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDAO, times(2)).save(captor.capture());

        List<Notification> capturedNotifications = captor.getAllValues();
        assertEquals("user1", capturedNotifications.get(0).getUser().getId());
        assertEquals("STAT priority Order with Lab Number LAB-101 has been Created for Tests : Full Blood Count",
                capturedNotifications.get(0).getMessage());

        assertEquals("user2", capturedNotifications.get(1).getUser().getId());
        assertEquals("STAT priority Order with Lab Number LAB-101 has been Created for Tests : Liver Function Test",
                capturedNotifications.get(1).getMessage());
    }

    @Test
    public void sendStatOrderNotifications_isNoOp_whenOrderIsNotStat() {
        SampleOrderItem orderItem = new SampleOrderItem();
        orderItem.setLabNo("LAB-102");
        orderItem.setPriority(OrderPriority.ROUTINE);

        controller.sendStatOrderNotifications(orderItem);

        verify(notificationDAO, never()).save(any());
    }

    @Test
    public void sendStatOrderNotifications_isNoOp_whenOrderIsNull() {
        controller.sendStatOrderNotifications(null);

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
