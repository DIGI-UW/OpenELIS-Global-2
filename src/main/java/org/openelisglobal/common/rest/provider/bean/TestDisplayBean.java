package org.openelisglobal.common.rest.provider.bean;

import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class TestDisplayBean extends IdValuePair {

    String resultType;
    List<IdValuePair> resultList;

    public TestDisplayBean(String id, String value, String resultType) {
        super(id, value);
        this.resultType = resultType;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public List<IdValuePair> getResultList() {
        return resultList;
    }

    public void setResultList(List<IdValuePair> resultList) {
        this.resultList = resultList;
    }
}
