package org.openelisglobal.notifications.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Security;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.dao.NotificationSubscriptionDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.notifications.entity.NotificationSubscriptions;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/rest")
@RestController
public class NotificationRestController extends BaseRestController {

    private final NotificationDAO notificationDAO;
    private final SystemUserService systemUserService;
    private final NotificationSubscriptionDAO notificationSubscriptionDAO;

    @Autowired
    private ConfigurableEnvironment env;

    @Autowired
    public NotificationRestController(NotificationDAO notificationDAO, SystemUserService systemUserService,
            NotificationSubscriptionDAO notificationSubscriptionDAO) {
        this.notificationDAO = notificationDAO;
        this.systemUserService = systemUserService;
        this.notificationSubscriptionDAO = notificationSubscriptionDAO;
    }

    @GetMapping("/notifications/all")
    public List<Notification> getNotifications() {
        return notificationDAO.getNotifications();
    }

    @GetMapping("/notifications")
    public List<Notification> getNotificationsByUserId(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        return notificationDAO.getNotificationsByUserId(Long.parseLong(sysUserId));
    }

    @PostMapping("/notification/{userId}")
    public ResponseEntity<?> saveNotification(@PathVariable String userId, @RequestBody Notification notification) {
        try {
            // Set user and created date
            notification.setUser(systemUserService.getUserById(userId));
            notification.setCreatedDate(OffsetDateTime.now());
            notification.setReadAt(null);

            // Save notification
            notificationDAO.save(notification);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save notification: " + e.getMessage());
        }

        try {
            // Ensure BouncyCastleProvider is added for cryptographic operations
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add BouncyCastleProvider: " + e.getMessage());
        }

        NotificationSubscriptions ns;
        try {
            // Get notification subscription by userId
            ns = notificationSubscriptionDAO.getNotificationSubscriptionByUserId(Long.valueOf(userId));
            if (ns == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subscription not found for userId: " + userId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve notification subscription: " + e.getMessage());
        }

        try {
            // Configure PushService with VAPID keys
            PushService pushService = new PushService(env.getProperty("vapid.public.key"),
                    env.getProperty("vapid.private.key"), "mailto:your-email@example.com");

            String title = "OpenELIS Global Notification";
            String body = notification.getMessage();
            String url = "http://localhost";

            // Create a notification message
            Map<String, String> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            payload.put("url", url);

            ObjectMapper objectMapper = new ObjectMapper();
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Use fully qualified name for web push Notification
            nl.martijndwars.webpush.Notification webPushNotification = new nl.martijndwars.webpush.Notification(
                    ns.getPfEndpoint(), ns.getPfP256dh(), ns.getPfAuth(), payloadJson);

            // Send push notification
            HttpResponse response = pushService.send(webPushNotification);

            return ResponseEntity.ok()
                    .body("Push notification sent successfully. Response: " + response.getStatusLine());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send push notification: " + e.getMessage());
        }
    }

    @GetMapping("/notification/pnconfig")
    public ResponseEntity<?> getSubscriptionDetails(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        NotificationSubscriptions ns = notificationSubscriptionDAO
                .getNotificationSubscriptionByUserId(Long.valueOf(sysUserId));

        if (ns == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subscription not found");
        }

        return ResponseEntity.ok().body(ns);

    }

    @PutMapping("/notification/markasread/{id}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable String id) {
        Notification notification = notificationDAO.getNotificationById(Long.parseLong(id));
        notification.setReadAt(OffsetDateTime.now());
        notificationDAO.updateNotification(notification);
        return ResponseEntity.ok().body("Notification updated successfully");
    }

    @PutMapping("/notification/markasread/all")
    public ResponseEntity<?> markAllNotificationsAsRead(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        notificationDAO.setAllUserNotificationsToRead(Long.valueOf(sysUserId));
        return ResponseEntity.ok().body("All notifications updated successfully");
    }

    @GetMapping("/systemusers")
    public List<SystemUser> getSystemUsers() {
        return notificationDAO.getSystemUsers();
    }

    @GetMapping("/notification/public_key")
    public ResponseEntity<Map<String, String>> getPublicKey() {

        String publicKey = env.getProperty("vapid.public.key");

        Map<String, String> response = new HashMap<>();
        response.put("publicKey", publicKey);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/notification/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody NotificationSubscriptions notificationSubscription,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);

        // Fetch the user object
        SystemUser user = systemUserService.getUserById(sysUserId);

        // Ensure user object is not null
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // Set the user entity directly
        notificationSubscription.setUser(user);

        notificationSubscriptionDAO.saveOrUpdate(notificationSubscription);

        return ResponseEntity.ok().body("Subscribed successfully");
    }

    @PutMapping("/notification/unsubscribe")
    public ResponseEntity<?> unsubscribe(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        NotificationSubscriptions ns = notificationSubscriptionDAO
                .getNotificationSubscriptionByUserId(Long.valueOf(sysUserId));

        if (ns == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subscription not found");
        }

        notificationSubscriptionDAO.delete(ns);

        return ResponseEntity.ok().body("Unsubscribed successfully");
    }
}
