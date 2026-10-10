package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;

public interface MicroCultureDAO {
    List<MicroCaseInoculation> getRows(String caseId);

    java.util.List<org.openelisglobal.sampleitem.valueholder.SampleItem> getSources(String caseId);

    void insert(Object entity);
}
