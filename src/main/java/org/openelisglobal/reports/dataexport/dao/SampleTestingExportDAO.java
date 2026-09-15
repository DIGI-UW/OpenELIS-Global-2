package org.openelisglobal.reports.dataexport.dao;

import java.util.List;
import java.util.stream.Stream;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

public interface SampleTestingExportDAO extends BaseDAO<Result, String> {
    Stream<Result> stream(ExportSnapshot request);

    List<Test> tests();

    List<TestResultComponent> components();

    Patient patient(String sampleId);

    long analysisCount(String sampleId);

    List<ObservationHistoryType> observationTypes();

    List<ObservationHistory> observations(String sampleId, String specimenId);

    String dictionary(String id);

    List<String> qualifiers(String resultId);

    void clearReadBatch();
}
