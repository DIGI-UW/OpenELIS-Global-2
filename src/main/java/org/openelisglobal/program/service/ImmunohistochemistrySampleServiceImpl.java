package org.openelisglobal.program.service;

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
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistrySampleForm;
import org.openelisglobal.program.dao.ImmunohistochemistrySampleDAO;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
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
public class ImmunohistochemistrySampleServiceImpl
        extends AuditableBaseObjectServiceImpl<ImmunohistochemistrySample, Integer>
        implements ImmunohistochemistrySampleService {
    @Autowired
    protected ImmunohistochemistrySampleDAO baseObjectDAO;
    @Autowired
    protected SystemUserService systemUserService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    ImmunohistochemistrySampleServiceImpl() {
        super(ImmunohistochemistrySample.class);
        this.auditTrailLog = true;
    }

    @Override
    protected ImmunohistochemistrySampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<ImmunohistochemistrySample> getWithStatus(List<ImmunohistochemistryStatus> statuses) {
        return baseObjectDAO.getWithStatus(statuses);
    }

    @Transactional
    @Override
    public void assignTechnician(Integer immunohistochemistrySampleId, SystemUser systemUser) {
        ImmunohistochemistrySample immunohistochemistrySample = get(immunohistochemistrySampleId);
        immunohistochemistrySample.setTechnician(systemUser);
    }

    @Transactional
    @Override
    public void assignPathologist(Integer immunohistochemistrySampleId, SystemUser systemUser) {
        ImmunohistochemistrySample immunohistochemistrySample = get(immunohistochemistrySampleId);
        immunohistochemistrySample.setPathologist(systemUser);
    }

    @Override
    public Long getCountWithStatus(List<ImmunohistochemistryStatus> statuses) {
        return baseObjectDAO.getCountWithStatus(statuses);
    }

    @Transactional
    @Override
    public void updateWithFormValues(Integer immunohistochemistrySampleId, ImmunohistochemistrySampleForm form) {
        ImmunohistochemistrySample immunohistochemistrySample = get(immunohistochemistrySampleId);
        if (!GenericValidator.isBlankOrNull(form.getAssignedPathologistId())) {
            immunohistochemistrySample.setPathologist(systemUserService.get(form.getAssignedPathologistId()));
        }
        if (!GenericValidator.isBlankOrNull(form.getAssignedTechnicianId())) {
            immunohistochemistrySample.setTechnician(systemUserService.get(form.getAssignedTechnicianId()));
        }
        immunohistochemistrySample.setStatus(form.getStatus());

        immunohistochemistrySample.getReports().removeAll(immunohistochemistrySample.getReports());
        if (form.getReports() != null)
            form.getReports().stream().forEach(e -> e.setId(null));
        immunohistochemistrySample.getReports().addAll(form.getReports());
        if (form.getRelease()) {
            validateImmunohistochemistrySample(immunohistochemistrySample, form);
        }
    }

    private void validateImmunohistochemistrySample(ImmunohistochemistrySample immunohistochemistrySample,
            ImmunohistochemistrySampleForm form) {
        immunohistochemistrySample.setStatus(ImmunohistochemistryStatus.COMPLETED);
        Sample sample = immunohistochemistrySample.getSample();
        Patient patient = sampleService.getPatient(sample);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(form.getSystemUserId());

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        List<TestResultItem> testResultItems = resultsUtility.getGroupedTestsForSample(sample);
        for (TestResultItem testResultItem : testResultItems) {
            if (!testResultItem.getIsGroupSeparator()) {
                if (ResultType.isTextOnlyVariant(testResultItem.getResultType())) {
                    testResultItem.setResultValue(MessageUtil.getMessage("result.immunochemistry.seereport"));
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
                    } else {
                        analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
                    }
                    actionDataSet.getNewResults()
                            .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));

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

    @Override
    public List<ImmunohistochemistrySample> searchWithStatusAndTerm(List<ImmunohistochemistryStatus> statuses,
            String searchTerm) {
        List<ImmunohistochemistrySample> immunohistochemistrySamples = baseObjectDAO.getWithStatus(statuses);
        if (StringUtils.isNotBlank(searchTerm)) {
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample != null) {
                immunohistochemistrySamples = baseObjectDAO.searchWithStatusAndAccesionNumber(statuses, searchTerm);
            } else {
                List<ImmunohistochemistrySample> filteredImmunohistochemistrySamples = new ArrayList<>();
                immunohistochemistrySamples.forEach(pathologySample -> {
                    Patient patient = sampleService.getPatient(pathologySample.getSample());
                    if (patient.getPerson().getFirstName().equals(searchTerm)
                            || patient.getPerson().getLastName().equals(searchTerm)) {
                        filteredImmunohistochemistrySamples.add(pathologySample);
                    }
                });
                immunohistochemistrySamples = filteredImmunohistochemistrySamples;
            }
        }

        return immunohistochemistrySamples;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<ImmunohistochemistryStatus> statuses, Timestamp from,
            Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }

    @Override
    public ImmunohistochemistrySample getByPathologySampleId(Integer pathologySampleId) {
        return baseObjectDAO.getByPathologySampleId(pathologySampleId);
    }
}
