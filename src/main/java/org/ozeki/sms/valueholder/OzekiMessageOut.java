package org.ozeki.sms.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// this class is made to work with ozeki SMS gateway using the database send mechanism
@Entity
@Table(name = "ozekimessageout")
public class OzekiMessageOut {

    @Id
    @GeneratedValue
    private Integer id;
    @Column
    private String sender;
    @Column
    private String receiver;
    @Column
    private String msg;
    @Column
    private String senttime;
    @Column
    private String receivedtime;
    @Column
    private String operator;
    @Column
    private String msgtype;
    @Column
    private String preference;
    @Column
    private String status;
    @Column
    private String errormsg;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSenttime() {
        return senttime;
    }

    public void setSenttime(String senttime) {
        this.senttime = senttime;
    }

    public String getReceivedtime() {
        return receivedtime;
    }

    public void setReceivedtime(String receivedtime) {
        this.receivedtime = receivedtime;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getErrormsg() {
        return errormsg;
    }

    public void setErrormsg(String errormsg) {
        this.errormsg = errormsg;
    }
}
