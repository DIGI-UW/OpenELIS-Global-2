package org.openelisglobal.dataexchange.orderresult.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

public class HL7MessageOut extends BaseObject<String> {

    public static final String FAIL = "fail";
    public static final String SUCCESS = "success";

    private String id;
    private String data;
    private String status;
    private Timestamp lastupdated;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public Timestamp getLastupdated() {
        return lastupdated;
    }

    @Override
    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }
}
