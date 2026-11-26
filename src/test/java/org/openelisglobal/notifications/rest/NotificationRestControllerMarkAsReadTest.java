package org.openelisglobal.notifications.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.dao.NotificationSubscriptionDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Issue #3274: PUT /rest/notification/markasread/{id} answered HTTP 500 for a
 * non-numeric id (NumberFormatException) and for an id with no notification
 * (NullPointerException). It now answers 400 and 404.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationRestControllerMarkAsReadTest {

    @Mock
    private NotificationDAO notificationDAO;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private NotificationSubscriptionDAO notificationSubscriptionDAO;

    @InjectMocks
    private NotificationRestController controller;

    @Test
    public void markAsRead_nonNumericId_returnsBadRequest() {
        ResponseEntity<?> response = controller.markNotificationAsRead("abc");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(notificationDAO, never()).getNotificationById(anyLong());
        verify(notificationDAO, never()).updateNotification(any());
    }

    @Test
    public void markAsRead_unknownId_returnsNotFound() {
        when(notificationDAO.getNotificationById(999999L)).thenReturn(null);

        ResponseEntity<?> response = controller.markNotificationAsRead("999999");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(notificationDAO, never()).updateNotification(any());
    }

    @Test
    public void markAsRead_existingId_setsReadAtAndReturnsOk() {
        Notification notification = new Notification();
        when(notificationDAO.getNotificationById(7L)).thenReturn(notification);

        ResponseEntity<?> response = controller.markNotificationAsRead("7");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(notification.getReadAt());
        verify(notificationDAO).updateNotification(notification);
    }
}
