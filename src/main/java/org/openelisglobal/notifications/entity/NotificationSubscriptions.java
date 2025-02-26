package org.openelisglobal.notifications.entity;

import jakarta.persistence.*;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Entity
@Table(name = "notification_subscriptions")
public class NotificationSubscriptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pf_endpoint", nullable = false)
    private String pfEndpoint;

    @Column(name = "pf_p256dh", nullable = false)
    private String pfP256dh;

    @Column(name = "pf_auth", nullable = false)
    private String pfAuth;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private SystemUser user;

    // Transient fields
    @Transient
    private String title;

    @Transient
    private String message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SystemUser getUser() {
        return user;
    }

    public void setUser(SystemUser user) {
        this.user = user;
    }

    public String getPfEndpoint() {
        return pfEndpoint;
    }

    public void setPfEndpoint(String pfEndpoint) {
        this.pfEndpoint = pfEndpoint;
    }

    public String getPfP256dh() {
        return pfP256dh;
    }

    public void setPfP256dh(String pfP256dh) {
        this.pfP256dh = pfP256dh;
    }

    public String getPfAuth() {
        return pfAuth;
    }

    public void setPfAuth(String pfAuth) {
        this.pfAuth = pfAuth;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "NotificationSubscription{" + "userId=" + (user != null ? user.getId() : "null") + ", pfEndpoint='"
                + pfEndpoint + '\'' + ", pfP256dh='" + pfP256dh + '\'' + ", pfAuth='" + pfAuth + '\'' + ", user="
                + (user != null ? user.toString() : "null") + ", title='" + title + '\'' + ", message='" + message
                + '\'' + '}';
    }
}
