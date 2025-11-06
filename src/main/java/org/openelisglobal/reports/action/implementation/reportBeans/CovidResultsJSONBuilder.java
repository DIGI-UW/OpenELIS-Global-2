package org.openelisglobal.reports.action.implementation.reportBeans;

import ca.uhn.hl7v2.hoh.util.repackage.Base64.Charsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Task;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.result.valueholder.Result;

public class CovidResultsJSONBuilder extends CovidResultsBuilderImpl {

    public CovidResultsJSONBuilder(DateRange dateRange) {
        super(dateRange);
    }

    private JSONArray dataSource = new JSONArray();

    @Override
    public void buildDataSource() {
        List<Analysis> analysises = getCovidAnalysisWithinDate();
        for (Analysis analysis : analysises) {
            dataSource.put(getResultJSON(analysis));
        }
    }

    private JSONObject getResultJSON(Analysis analysis) {
        Optional<Result> result = getResultForAnalysis(analysis);
        Patient patient = getPatientForAnalysis(analysis);
        JSONObject resultJSON = new JSONObject();
        resultJSON.append(RESULT_PROPERTY_NAME, getResultValue(result));
        resultJSON.append(ORDER_NUMBER_PROPERTY_NAME, analysis.getSampleItem().getSample().getAccessionNumber());
        resultJSON.append(DATE_PROPERTY_NAME, analysis.getStartedDateForDisplay());

        resultJSON.append(PATIENT_LAST_NAME_PROPERTY_NAME, patient.getPerson().getLastName());
        resultJSON.append(PATIENT_FIRST_NAME_PROPERTY_NAME, patient.getPerson().getFirstName());
        resultJSON.append(PATIENT_DATE_OF_BIRTH_PROPERTY_NAME, patient.getBirthDateForDisplay());
        resultJSON.append(PATIENT_PHONE_NO_PROPERTY_NAME, patient.getPerson().getPrimaryPhone());

        Optional<Task> task = getReferringTaskForAnalysis(analysis);

        if (task.isPresent() && !GenericValidator.isBlankOrNull(task.get().getDescription())) {
            try {
                resultJSON.append(LOCATOR_FORM_PROPERTY_NAME, new JSONObject(task.get().getDescription()));
            } catch (JSONException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "getResultJSON",
                        "could not make json from task description");
                LogEvent.logError(e);
            }
        }
        return resultJSON;
    }

    @Override
    public byte[] getDataSourceAsByteArray() {
        return dataSource.toString(1).getBytes(Charsets.UTF_8);
    }
}
