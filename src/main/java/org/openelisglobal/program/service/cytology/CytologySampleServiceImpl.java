package org.openelisglobal.program.service.cytology;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.dao.cytology.CytologySampleDAO;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CytologySampleServiceImpl extends AuditableBaseObjectServiceImpl<CytologySample, Integer>
        implements CytologySampleService {

    @Autowired
    protected CytologySampleDAO baseObjectDAO;

    @Autowired
    protected SystemUserService systemUserService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    CytologySampleServiceImpl() {
        super(CytologySample.class);
        this.auditTrailLog = true;
    }

    @Override
    protected CytologySampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<CytologySample> getWithStatus(List<CytologyStatus> statuses) {
        return baseObjectDAO.getWithStatus(statuses);
    }

    @Transactional
    @Override
    public void assignTechnician(Integer cytologySampleId, SystemUser systemUser) {
        CytologySample cytologySample = get(cytologySampleId);
        cytologySample.setTechnician(systemUser);
    }

    @Transactional
    @Override
    public List<CytologySample> searchWithStatusAndTerm(List<CytologyStatus> statuses, String searchTerm) {
        List<CytologySample> cytologySamples = baseObjectDAO.getWithStatus(statuses);
        if (StringUtils.isNotBlank(searchTerm)) {
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample != null) {
                cytologySamples = baseObjectDAO.searchWithStatusAndAccesionNumber(statuses, searchTerm);
            } else {
                List<CytologySample> filteredCytologySamples = new ArrayList<>();
                cytologySamples.forEach(cytologySample -> {
                    Patient patient = sampleService.getPatient(cytologySample.getSample());
                    if (patient.getPerson().getFirstName().equals(searchTerm)
                            || patient.getPerson().getLastName().equals(searchTerm)) {
                        filteredCytologySamples.add(cytologySample);
                    }
                });
                cytologySamples = filteredCytologySamples;
            }
        }

        return cytologySamples;
    }

    @Transactional
    @Override
    public void assignCytoPathologist(Integer cytologySampleId, SystemUser systemUser) {
        CytologySample cytologySample = get(cytologySampleId);
        cytologySample.setCytoPathologist(systemUser);
        update(cytologySample);
    }

    @Override
    public Long getCountWithStatus(List<CytologyStatus> statuses) {
        return baseObjectDAO.getCountWithStatus(statuses);
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }

    @Transactional
    @Override
    public void updateWithFormValues(Integer cytologySampleId, CytologySampleForm form) {
        CytologySample cytologySample = get(cytologySampleId);
        if (!GenericValidator.isBlankOrNull(form.getAssignedCytoPathologistId())) {
            cytologySample.setCytoPathologist(systemUserService.get(form.getAssignedCytoPathologistId()));
        }
        if (!GenericValidator.isBlankOrNull(form.getAssignedTechnicianId())) {
            cytologySample.setTechnician(systemUserService.get(form.getAssignedTechnicianId()));
        }
        cytologySample.setStatus(form.getStatus());

        cytologySample.getSlides().removeAll(cytologySample.getSlides());
        if (form.getSlides() != null)
            form.getSlides().stream().forEach(e -> e.setId(null));
        cytologySample.getSlides().addAll(form.getSlides());
        if (form.getSpecimenAdequacy() != null) {
            cytologySample.setSpecimenAdequacy(form.getSpecimenAdequacy());
        }

        cytologySample.getReports().removeAll(cytologySample.getReports());
        if (form.getReports() != null) {
            form.getReports().stream().forEach(e -> e.setId(null));
            cytologySample.getReports().addAll(form.getReports());
        }

        if (form.getDiagnosis() != null) {
            cytologySample.setDiagnosis(form.getDiagnosis());
        }

        if (form.getRelease()) {
            validateCytologySample(cytologySample, form);
        }
    }

    private void validateCytologySample(CytologySample cytologySample, CytologySampleForm form) {
        cytologySample.setStatus(CytologyStatus.COMPLETED);
        Sample sample = cytologySample.getSample();
        Patient patient = sampleService.getPatient(sample);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(form.getSystemUserId());

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        List<TestResultItem> testResultItems = resultsUtility.getGroupedTestsForSample(sample);
        for (TestResultItem testResultItem : testResultItems) {
            if (!testResultItem.getIsGroupSeparator()) {
                if (ResultType.isTextOnlyVariant(testResultItem.getResultType())) {
                    testResultItem.setResultValue(MessageUtil.getMessage("result.cytoology.seereport"));
                }
                Analysis analysis = analysisService.get(sample.getId());
                ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
                ResultSaveService resultSaveService = new ResultSaveService(analysis, form.getSystemUserId());
                List<Result> results = resultSaveService.createResultsFromTestResultItem(bean, new ArrayList<>());
                for (Result result : results) {
                    boolean newResult = result.getId() == null;
                    analysis.setEnteredDate(DateUtil.getNowAsTimestamp());

                    if (newResult) {
                        analysis.setRevision("1");
                        actionDataSet.getNewResults()
                                .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                    } else {
                        analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
                        actionDataSet.getModifiedResults()
                                .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                    }

                    // analysis.setStartedDateForDisplay(testResultItem.getTestDate());

                    // This needs to be refactored -- part of the logic is in
                    // getStatusForTestResult. RetroCI over rides to whatever was set before
                    if (ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules)
                            .equals(IActionConstants.STATUS_RULES_RETROCI)) {
                        if (!SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)
                                .equals(analysis.getStatusId())) {
                            analysis.setCompletedDate(
                                    DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
                            analysis.setStatusId(SpringContext.getBean(IStatusService.class)
                                    .getStatusID(AnalysisStatus.TechnicalAcceptance));
                        }
                    } else if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                            AnalysisStatus.Finalized)
                            || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                                    AnalysisStatus.TechnicalAcceptance)
                            || (analysis.isReferredOut()
                                    && !GenericValidator.isBlankOrNull(testResultItem.getShadowResultValue()))) {
                        analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
                        analysis.setStatusId(
                                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                    }

                    // this code is pulled from LogbookResultsRestController
                    // addResult(result, testResultItem, analysis, results.size() > 1,
                    // actionDataSet, useTechnicianName);
                    //
                    // if (analysisShouldBeUpdated(testResultItem, result, supportReferrals)) {
                    // updateAnalysis(testResultItem, testResultItem.getTestDate(),
                    // analysis, statusRuleSet);
                    // }
                }
                analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                analysis.setReleasedDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
            }
        }

        logbookResultsPersistService.persistDataSet(actionDataSet, ResultUpdateRegister.getRegisteredUpdaters(),
                form.getSystemUserId());
        sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
    }
}
