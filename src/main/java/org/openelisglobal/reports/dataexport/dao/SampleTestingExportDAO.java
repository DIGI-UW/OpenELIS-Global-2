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
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

public interface SampleTestingExportDAO extends BaseDAO<Result, String> {
    Stream<Result> stream(ExportSnapshot request);

    List<Test> tests();

    /** Active components only: what a user may select as a report column. */
    List<TestResultComponent> components();

    /**
     * Every component, including deactivated ones. Removing a component from test
     * configuration soft-deactivates it while existing {@code Result} rows keep
     * referencing it, so historical results must be resolved against the full set.
     * Resolving against {@link #components()} instead leaves those results with a
     * null component, which drops their label and folds their value into the
     * overall test measurement.
     */
    List<TestResultComponent> allComponents();

    /** Units referenced by {@link TestResultComponent#getUomId()}. */
    List<UnitOfMeasure> unitsOfMeasure();

    Patient patient(String sampleId);

    long analysisCount(String sampleId);

    List<ObservationHistoryType> observationTypes();

    List<ObservationHistory> observations(String sampleId, String specimenId);

    String dictionary(String id);

    List<String> qualifiers(String resultId);

    void clearReadBatch();
}
