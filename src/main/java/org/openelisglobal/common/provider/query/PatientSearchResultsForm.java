package org.openelisglobal.common.provider.query;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.PagingBean;

public class PatientSearchResultsForm implements IPagingForm {

    private PagingBean paging;

    @Getter
    @Setter
    private List<PatientSearchResults> patientSearchResults;

    @Override
    public void setPaging(PagingBean pagingBean) {
        this.paging = pagingBean;
    }

    @Override
    public PagingBean getPaging() {
        return paging;
    }
}
