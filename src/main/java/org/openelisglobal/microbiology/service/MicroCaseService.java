package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

public interface MicroCaseService {

    MicroCase createOrGetCase(SampleItem sampleItem, Test test, String performedBy);

    MicroCase getCase(String caseId);

    List<MicroCaseSpecimen> getSpecimens(String caseId);

    List<String> getSpecimenIds(String caseId);

    List<MicroCase> getSiblingCases(String sampleItemId);

    MicroCaseDetailForm getCaseDetail(String caseId);
}
