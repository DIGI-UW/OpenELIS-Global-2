package org.openelisglobal.resultvalidation.form;

import java.util.List;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;

public interface IValidationForm {

    List<AnalysisItem> getResultList();

    void setResultList(List<AnalysisItem> resultList);

    void setTestSectionId(String string);

    String getTestSectionId();

    boolean getSearchFinished();
}
