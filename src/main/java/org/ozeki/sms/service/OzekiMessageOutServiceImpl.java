package org.ozeki.sms.service;

import org.openelisglobal.notification.valueholder.SMSNotification;
import org.ozeki.sms.dao.OzekiMessageOutDAO;
import org.ozeki.sms.valueholder.OzekiMessageOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OzekiMessageOutServiceImpl implements OzekiMessageOutService {

    @Autowired
    private OzekiMessageOutDAO dao;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(SMSNotification notification) {
        OzekiMessageOut messageOut = new OzekiMessageOut();
        messageOut.setMsg(notification.getMessage());
        messageOut.setReceiver(notification.getReceiverPhoneNumber());
        messageOut.setStatus("send");
        dao.save(messageOut);
    }
}
