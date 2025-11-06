package org.openelisglobal.referral.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referral.action.beanitems.ReferralDisplayItem;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.form.ReferredOutTestsForm.ReferDateType;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralServiceImpl extends AuditableBaseObjectServiceImpl<Referral, String> implements ReferralService {
    @Autowired
    protected ReferralDAO baseObjectDAO;

    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private AnalysisService analysisService;

    ReferralServiceImpl() {
        super(Referral.class);
    }

    @Override
    protected ReferralDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Referral getReferralByAnalysisId(String id) {
        return getMatch("analysis.id", id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Referral> getUncanceledOpenReferrals() {
        return getBaseObjectDAO().getReferralsByStatus(
                Arrays.asList(ReferralStatus.CREATED, ReferralStatus.SENT, ReferralStatus.RECEIVED));
    }

    @Override
    @Transactional(readOnly = true)
    public Referral getReferralById(String referralId) {
        return getBaseObjectDAO().getReferralById(referralId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Referral> getReferralsBySampleId(String id) {
        return getBaseObjectDAO().getAllReferralsBySampleId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Referral> getReferralsByOrganization(String organizationId, Date lowDate, Date highDate) {
        return getBaseObjectDAO().getAllReferralsByOrganization(organizationId, lowDate, highDate);
    }

    @Override
    public List<Referral> getSentReferrals() {
        return getBaseObjectDAO().getReferralsByStatus(Arrays.asList(ReferralStatus.SENT));
    }

    @Override
    public List<UUID> getSentReferralUuids() {
        return getBaseObjectDAO().getReferralsByStatus(Arrays.asList(ReferralStatus.SENT)).stream()
                .map(e -> e.getFhirUuid()).filter(e -> e != null).collect(Collectors.toList());
    }

    @Override
    public List<Referral> getReferralsByTestAndDate(ReferDateType dateType, Timestamp startTimestamp,
            Timestamp endTimestamp, List<String> testUnitIds, List<String> testIds) {
        return baseObjectDAO.getReferralsByTestAndDate(dateType, startTimestamp, endTimestamp, testUnitIds, testIds);
    }

    @Override
    public List<Referral> getReferralsByAccessionNumber(String labNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
        if (sample != null) {
            List<Analysis> analysises = analysisService.getAnalysesBySampleId(sample.getId());
            return baseObjectDAO
                    .getReferralsByAnalysisIds(analysises.stream().map(e -> e.getId()).collect(Collectors.toList()));
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public List<Referral> getReferralByPatientId(String selPatient) {
        List<Sample> samples = sampleHumanService.getSamplesForPatient(selPatient);
        List<Analysis> analysises = new ArrayList<>();
        for (Sample sample : samples) {
            analysises.addAll(analysisService.getAnalysesBySampleId(sample.getId()));
        }
        return baseObjectDAO
                .getReferralsByAnalysisIds(analysises.stream().map(e -> e.getId()).collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralDisplayItem> getReferralItems(ReferredOutTestsForm form) {
        List<ReferralDisplayItem> referralItems = new ArrayList<>();
        List<Referral> referrals;
        switch (form.getSearchType()) {
        case TEST_AND_DATES:
            referrals = getReferralsByTestAndDate(form);
            break;
        case LAB_NUMBER:
            referrals = getReferralsByLabNumber(form);
            break;
        case PATIENT:
            referrals = getReferralsByPatient(form);
            break;
        default:
            referrals = new ArrayList<>();
        }

        for (Referral referral : referrals) {
            referralItems.add(convertToDisplayItem(referral));
        }

        return referralItems;
    }

    private List<Referral> getReferralsByTestAndDate(ReferredOutTestsForm form) {
        String startDate = form.getStartDate();
        String endDate = form.getEndDate();
        if (GenericValidator.isBlankOrNull(startDate) && !GenericValidator.isBlankOrNull(endDate)) {
            startDate = endDate;
        }
        if (GenericValidator.isBlankOrNull(endDate) && !GenericValidator.isBlankOrNull(startDate)) {
            endDate = startDate;
        }
        java.sql.Timestamp startTimestamp = GenericValidator.isBlankOrNull(startDate) ? null
                : DateUtil.convertStringDateStringTimeToTimestamp(startDate, "00:00:00.0");
        java.sql.Timestamp endTimestamp = GenericValidator.isBlankOrNull(endDate) ? null
                : DateUtil.convertStringDateStringTimeToTimestamp(endDate, "23:59:59");
        return getReferralsByTestAndDate(form.getDateType(), startTimestamp, endTimestamp, form.getTestUnitIds(),
                form.getTestIds());
    }

    private List<Referral> getReferralsByLabNumber(ReferredOutTestsForm form) {
        return getReferralsByAccessionNumber(form.getLabNumber());
    }

    private List<Referral> getReferralsByPatient(ReferredOutTestsForm form) {
        return getReferralByPatientId(form.getSelPatient());
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralDisplayItem convertToDisplayItem(Referral referral) {
        ReferralDisplayItem referralItem = new ReferralDisplayItem();

        Analysis analysis = referral.getAnalysis();
        List<Result> resultList = analysisService.getResults(analysis);
        Patient patient = sampleHumanService.getPatientForSample(analysis.getSampleItem().getSample());

        referralItem.setAccessionNumber(analysis.getSampleItem().getSample().getAccessionNumber());
        referralItem.setReferredSendDate(DateUtil.convertTimestampToStringDate(referral.getSentDate()));
        referralItem.setReferralStatus(referral.getStatus());
        referralItem.setReferralStatusDisplay(referral.getStatus().toString());
        referralItem.setPatientLastName(patient.getPerson().getLastName());
        referralItem.setPatientFirstName(patient.getPerson().getFirstName());
        referralItem.setReferringTestName(analysis.getTest().getLocalizedTestName().getLocalizedValue());
        if (!resultList.isEmpty()) {
            referralItem.setReferralResultsDisplay(getAppropriateResultValue(resultList));
            referralItem.setResultDate(analysis.getCompletedDateForDisplay());
        }
        Organization organization = referral.getOrganization();
        if (organization != null) {
            referralItem.setReferenceLabDisplay(organization.getOrganizationName());
        }
        referralItem.setNotes(analysisService.getNotesAsString(analysis, true, true, "<br/>", false));
        referralItem.setAnalysisId(analysis.getId());

        return referralItem;
    }

    private String getAppropriateResultValue(List<Result> results) {
        Result result = results.get(0);
        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            if (!GenericValidator.isBlankOrNull(result.getValue()) && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.get(result.getValue());
                if (dictionary != null) {
                    return dictionary.getLocalizedName();
                }
            }
        } else if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())) {
            StringBuilder multiResult = new StringBuilder();

            for (Result subResult : results) {
                if (!GenericValidator.isBlankOrNull(result.getValue()) && !"0".equals(result.getValue())) {
                    Dictionary dictionary = dictionaryService.get(subResult.getValue());

                    if (dictionary.getId() != null) {
                        multiResult.append(dictionary.getLocalizedName());
                        multiResult.append(", ");
                    }
                }
            }

            if (multiResult.length() > 0) {
                multiResult.setLength(multiResult.length() - 2); // remove last ", "
            }

            return multiResult.toString();
        } else {
            String resultValue = GenericValidator.isBlankOrNull(result.getValue()) ? "" : result.getValue();

            if (!GenericValidator.isBlankOrNull(resultValue)
                    && result.getAnalysis().getTest().getUnitOfMeasure() != null) {
                resultValue += " " + result.getAnalysis().getTest().getUnitOfMeasure().getName();
            }

            return resultValue;
        }

        return "";
    }
}
