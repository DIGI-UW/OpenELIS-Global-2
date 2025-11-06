package org.ozeki.sms.service;

import org.openelisglobal.notification.valueholder.SMSNotification;

public interface OzekiMessageOutService {

    void send(SMSNotification smsNotification);
}
