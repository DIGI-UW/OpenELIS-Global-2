package org.openelisglobal.reports.action.implementation;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ReportTrackingService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.EIDReportData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public abstract class PatientEIDReport extends RetroCIPatientReport {

    protected static final long YEAR = 1000L * 60L * 60L * 24L * 365L;
    protected static final long THREE_YEARS = YEAR * 3L;
    protected static final long WEEK = YEAR / 52L;
    protected static final long MONTH = YEAR / 12L;

    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private ResultService resultService = SpringContext.getBean(ResultService.class);
    private SampleOrganizationService orgService = SpringContext.getBean(SampleOrganizationService.class);

    protected List<EIDReportData> reportItems;
    private String invalidValue = MessageUtil.getMessage("report.test.status.inProgress");

    @Override
    protected void initializeReportItems() {
        reportItems = new ArrayList<>();
    }

    @Override
    protected String getReportNameForReport() {
        return MessageUtil.getMessage("reports.label.patient.EID");
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }

        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected void createReportItems() {
        EIDReportData data = new EIDReportData();

        setPatientInfo(data);
        setTestInfo(data);
        reportItems.add(data);
    }

    protected void setTestInfo(EIDReportData data) {
        boolean atLeastOneAnalysisNotValidated = false;
        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(reportSample.getId());
        Timestamp lastReport = SpringContext.getBean(IReportTrackingService.class)
                .getTimeOfLastNamedReport(reportSample, ReportTrackingService.ReportType.PATIENT, requestedReport);
        Boolean mayBeDuplicate = lastReport != null;

        Date maxCompleationDate = null;
        long maxCompleationTime = 0L;
        String invalidValue = MessageUtil.getMessage("report.test.status.inProgress");

        for (Analysis analysis : analysisList) {

            if (analysis.getCompletedDate() != null) {
                if (analysis.getCompletedDate().getTime() > maxCompleationTime) {
                    maxCompleationDate = analysis.getCompletedDate();
                    maxCompleationTime = maxCompleationDate.getTime();
                }
            }

            String testName = TestServiceImpl.getUserLocalizedTestName(analysis.getTest());

            List<Result> resultList = resultService.getResultsByAnalysis(analysis);

            boolean valid = ANALYSIS_FINALIZED_STATUS_ID.equals(analysis.getStatusId());
            if (!valid) {
                atLeastOneAnalysisNotValidated = true;
            }

            if (testName.equals("DNA PCR")) {
                if (valid) {
                    String resultValue = "";
                    if (resultList.size() > 0) {
                        resultValue = resultList.get(resultList.size() - 1).getValue();
                    }
                    Dictionary dictionary = new Dictionary();
                    dictionary.setId(resultValue);
                    dictionaryService.getData(dictionary);
                    data.setHiv_status(dictionary.getDictEntry());
                } else {
                    data.setHiv_status(invalidValue);
                }
            }
            if (mayBeDuplicate
                    && SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                            AnalysisStatus.Finalized)
                    && lastReport != null && lastReport.before(analysis.getLastupdated())) {
                mayBeDuplicate = false;
            }
        }
        if (maxCompleationDate != null) {
            data.setCompleationdate(DateUtil.convertSqlDateToStringDate(maxCompleationDate));
        }

        String observation = getObservationValues(OBSERVATION_WHICH_PCR_ID);

        if (!GenericValidator.isBlankOrNull(observation)) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId(observation);
            dictionaryService.getData(dictionary);
            data.setPcr_type(dictionary.getDictEntry());
        }
        data.setDuplicateReport(mayBeDuplicate);
        data.setStatus(atLeastOneAnalysisNotValidated ? MessageUtil.getMessage("report.status.partial")
                : MessageUtil.getMessage("report.status.complete"));
    }

    protected void setPatientInfo(EIDReportData data) {

        data.setSubjectno(reportPatient.getNationalId());
        data.setSitesubjectno(reportPatient.getExternalId());
        data.setBirth_date(reportPatient.getBirthDateForDisplay());
        data.setGender(reportPatient.getGender());
        data.setCollectiondate(DateUtil.convertTimestampToStringDateAndTime(reportSample.getCollectionDate()));
        SampleOrganization sampleOrg = new SampleOrganization();
        sampleOrg.setSample(reportSample);
        orgService.getDataBySample(sampleOrg);
        data.setServicename(sampleOrg.getId() == null ? "" : sampleOrg.getOrganization().getOrganizationName());
        data.setDoctor(getObservationValues(OBSERVATION_REQUESTOR_ID));
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            data.setAccessionNumber(
                    AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(reportSample.getAccessionNumber()));
        } else {
            data.setAccessionNumber(reportSample.getAccessionNumber());
        }
        data.setReceptiondate(DateUtil.convertTimestampToStringDateAndTime(reportSample.getReceivedTimestamp()));

        Timestamp collectionDate = reportSample.getCollectionDate();

        if (collectionDate != null) {
            long collectionTime = collectionDate.getTime() - reportPatient.getBirthDate().getTime();

            if (collectionTime < THREE_YEARS) {
                data.setAgeWeek(String.valueOf((int) Math.floor(collectionTime / WEEK)));
            } else {
                data.setAgeMonth(String.valueOf((int) Math.floor(collectionTime / MONTH)));
            }
        }
        data.getSampleQaEventItems(reportSample);
    }

    @Override
    protected String getProjectId() {
        return EID_STUDY_ID;
    }
}
