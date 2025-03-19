/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import jakarta.annotation.PostConstruct;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.services.TestIdentityService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.service.PatientServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.referral.service.ReferralReasonService;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;

public abstract class PatientReport extends Report {

    // not threadSafe, use this classes formatTwoDecimals method when using this
    private static final DecimalFormat twoDecimalFormat = new DecimalFormat("#.##");

    private static String ADDRESS_DEPT_ID;
    private static String ADDRESS_COMMUNE_ID;
    protected String currentContactInfo = "";
    protected String currentSiteInfo = "";

    protected SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    protected DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    protected SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected PatientService patientService = SpringContext.getBean(PatientService.class);
    protected PersonService personService = SpringContext.getBean(PersonService.class);
    protected ProviderService providerService = SpringContext.getBean(ProviderService.class);
    protected TestService testService = SpringContext.getBean(TestService.class);
    protected ReferralReasonService referralReasonService = SpringContext.getBean(ReferralReasonService.class);
    protected ReferralService referralService = SpringContext.getBean(ReferralService.class);
    protected ReferralResultService referralResultService = SpringContext.getBean(ReferralResultService.class);
    protected ObservationHistoryService observationService = SpringContext.getBean(ObservationHistoryService.class);
    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    protected NoteService noteService = SpringContext.getBean(NoteService.class);
    protected PersonAddressService addressService = SpringContext.getBean(PersonAddressService.class);
    protected AddressPartService addressPartService = SpringContext.getBean(AddressPartService.class);
    protected OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    protected SampleOrganizationService sampleOrganizationService = SpringContext
            .getBean(SampleOrganizationService.class);
    protected UserService userService = SpringContext.getBean(UserService.class);;
    private List<String> handledOrders;
    private List<Analysis> updatedAnalysis = new ArrayList<>();

    private String lowerNumber;
    private String upperNumber;
    protected String STNumber = null;
    protected String subjectNumber = null;
    protected String healthRegion = null;
    protected String healthDistrict = null;
    protected String patientName = null;
    protected String patientDOB = null;
    protected String currentConclusion = null;

    protected String patientDept = null;
    protected String patientCommune = null;

    protected Provider currentProvider;
    protected Analysis currentAnalysis;
    protected String reportReferralResultValue;
    protected List<ClinicalPatientData> reportItems;
    protected String completionDate;
    protected Sample currentSample;
    protected Patient currentPatient;
    protected Boolean onlyResultsForReportBySite = false;

    protected static final NoteType[] FILTER = { NoteType.EXTERNAL, NoteType.REJECTION_REASON,
            NoteType.NON_CONFORMITY };
    protected Map<String, Boolean> sampleCompleteMap;
    protected Map<String, Boolean> sampleCorrectedMap;

    @PostConstruct
    private void initialize() {
        List<AddressPart> partList = addressPartService.getAll();
        for (AddressPart part : partList) {
            if ("department".equals(part.getPartName())) {
                ADDRESS_DEPT_ID = part.getId();
            } else if ("commune".equals(part.getPartName())) {
                ADDRESS_COMMUNE_ID = part.getId();
            }
        }
    }

    protected abstract String getReportNameForParameterPage();

    protected abstract void postSampleBuild();

    protected abstract void createReportItems();

    protected abstract void setReferredResult(ClinicalPatientData data, Result result);

    protected boolean appendUOMToRange() {
        return true;
    }

    protected boolean augmentResultWithFlag() {
        return true;
    }

    protected boolean useReportingDescription() {
        return true;
    }

    protected String convertToAlphaNumericDisplay(Sample currentSample) {
        String displayAccesionNumber = "";
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            displayAccesionNumber = AlphanumAccessionValidator
                    .convertAlphaNumLabNumForDisplay(sampleService.getAccessionNumber(currentSample).split("-")[0]);
        } else {
            displayAccesionNumber = sampleService.getAccessionNumber(currentSample).split("-")[0];
        }
        return displayAccesionNumber;
    }

    public void setRequestParameters(ReportForm form) {
        form.setReportName(getReportNameForParameterPage());

        form.setUsePatientSearch(true);
        form.setPatientSearch(new PatientSearch());
        form.getPatientSearch().setDefaultHeader(false);
        form.setUseAccessionDirect(Boolean.TRUE);
        form.setUseHighAccessionDirect(Boolean.TRUE);
        form.setUseSiteSearch(true);
        form.setReferringSiteList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_PATIENT_REFERRING_CLINIC));
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;

        lowerNumber = form.getAccessionDirectNoSuffix();
        upperNumber = form.getHighAccessionDirectNoSuffix();

        handledOrders = new ArrayList<>();

        createReportParameters();

        boolean valid;
        List<Sample> reportSampleList = new ArrayList<>();

        if (form.getAnalysisIds() != null && form.getAnalysisIds().size() > 0) {
            reportSampleList = findReportSamples(form.getAnalysisIds());
        } else if (!GenericValidator.isBlankOrNull(lowerNumber) || !GenericValidator.isBlankOrNull(upperNumber)) {
            valid = validateAccessionNumbers();
            if (valid) {
                reportSampleList = findReportSamples(lowerNumber, upperNumber);
            }
        } else if (!GenericValidator.isBlankOrNull(form.getSelPatient())) {
            List<Patient> patientList = new ArrayList<>();
            valid = findPatientById(form.getSelPatient(), patientList);
            if (valid) {
                reportSampleList = findReportSamplesForReportPatient(patientList);
            }
        } else if (!GenericValidator.isBlankOrNull(form.getPatientNumberDirect())) {
            List<Patient> patientList = new ArrayList<>();
            valid = findPatientByPatientNumber(form.getPatientNumberDirect(), patientList);

            if (valid) {
                reportSampleList = findReportSamplesForReportPatient(patientList);
            }
        } else if (!GenericValidator.isBlankOrNull(form.getReferringSiteId())) {
            if (GenericValidator.isBlankOrNull(form.getUpperDateRange())
                    && !GenericValidator.isBlankOrNull(form.getLowerDateRange())) {
                form.setUpperDateRange(form.getLowerDateRange());
            }
            if (!GenericValidator.isBlankOrNull(form.getUpperDateRange())
                    && !GenericValidator.isBlankOrNull(form.getLowerDateRange())) {
                onlyResultsForReportBySite = form.isOnlyResults();
                reportSampleList = findReportSamplesForSite(form.getReferringSiteId(),
                        form.getReferringSiteDepartmentId(), form.isOnlyResults(), form.getDateType(),
                        form.getLowerDateRange(), form.getUpperDateRange());
            }
        }

        sampleCompleteMap = new HashMap<>();
        sampleCorrectedMap = new HashMap<>();
        initializeReportItems();

        if (reportSampleList.isEmpty()) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        } else {

            for (Sample sample : reportSampleList) {
                currentSample = sample;
                handledOrders.add(sample.getId());
                sampleCompleteMap.put(convertToAlphaNumericDisplay(sample), Boolean.TRUE);
                findCompletionDate();
                findPatientFromSample();
                findContactInfo();
                findPatientInfo();
                createReportItems();
            }
            if (reportItems.size() == 0) {
                add1LineErrorMessage("report.error.message.noPrintableItems");
            } else {
                postSampleBuild();
            }
        }

        if (!updatedAnalysis.isEmpty()) {
            try {
                analysisService.updateAllNoAuditTrail(updatedAnalysis);
                // for (Analysis analysis : updatedAnalysis) {
                // analysisService.update(analysis, true);
                // }

            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }

    private List<Sample> findReportSamplesForSite(String referringSiteId, String referringSiteDepartmentId,
            boolean onlyResults, DateType dateType, String lowerDateRange, String upperDateRange) {
        List<Sample> sampleList = new ArrayList<>();
        String sampleRequesterOrgId = GenericValidator.isBlankOrNull(referringSiteDepartmentId) ? referringSiteId
                : referringSiteDepartmentId;

        if (DateType.ORDER_DATE.equals(dateType)) {
            sampleList = sampleService.getSamplesForSiteBetweenOrderDates(sampleRequesterOrgId,
                    DateUtil.convertStringDateToLocalDate(lowerDateRange),
                    DateUtil.convertStringDateToLocalDate(upperDateRange));
        } else {
            List<Analysis> analysises = analysisService.getAnalysisForSiteBetweenResultDates(sampleRequesterOrgId,
                    DateUtil.convertStringDateToLocalDate(lowerDateRange),
                    DateUtil.convertStringDateToLocalDate(upperDateRange));
            sampleList = sampleService
                    .getSamplesByAnalysisIds(analysises.stream().map(e -> e.getId()).collect(Collectors.toList()));
        }

        if (onlyResults) {
            Set<Integer> analysisStatusIds = new HashSet<>();
            analysisStatusIds.add(Integer
                    .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
            sampleList = sampleList.stream().filter(
                    e -> (analysisService.getAnalysesBySampleIdAndStatusId(e.getId(), analysisStatusIds).size() > 0))
                    .collect(Collectors.toList());
        }

        return sampleList;
    }

    private boolean findPatientById(String patientId, List<Patient> patientList) {
        patientList.add(patientService.get(patientId));
        return !patientList.isEmpty();
    }

    private void findCompletionDate() {
        Date date = sampleService.getCompletedDate(currentSample);
        completionDate = date == null ? null : DateUtil.convertSqlDateToStringDate(date);
    }

    private void findPatientInfo() {
        if (patientService.getPerson(currentPatient) == null) {
            return;
        }

        patientDept = "";
        patientCommune = "";
        if (ADDRESS_DEPT_ID != null) {
            PersonAddress deptAddress = addressService
                    .getByPersonIdAndPartId(patientService.getPerson(currentPatient).getId(), ADDRESS_DEPT_ID);

            if (deptAddress != null && !GenericValidator.isBlankOrNull(deptAddress.getValue())) {
                patientDept = dictionaryService.getDictionaryById(deptAddress.getValue()).getDictEntry();
            }
        }

        if (ADDRESS_COMMUNE_ID != null) {
            PersonAddress deptAddress = addressService
                    .getByPersonIdAndPartId(patientService.getPerson(currentPatient).getId(), ADDRESS_COMMUNE_ID);

            if (deptAddress != null) {
                patientCommune = deptAddress.getValue();
            }
        }
    }

    private void findContactInfo() {
        currentContactInfo = "";
        currentSiteInfo = "";
        currentProvider = null;

        // sampleService.getOrganizationRequester(currentSample);
        Organization referringOrg = sampleService.getOrganizationRequester(currentSample,
                TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
        Organization referringDepartmentOrg = sampleService.getOrganizationRequester(currentSample,
                TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID);

        currentSiteInfo += referringOrg == null ? "" : referringOrg.getOrganizationName();
        currentSiteInfo += "|" + (referringDepartmentOrg == null ? "" : referringDepartmentOrg.getOrganizationName());

        // Person person = sampleService.getPersonRequester(currentSample);
        Person person = (ObjectUtils.isNotEmpty(sampleHumanService.getProviderForSample(currentSample)))
                ? sampleHumanService.getProviderForSample(currentSample).getPerson()
                : null;

        if (person != null) {
            PersonService personService = SpringContext.getBean(PersonService.class);
            currentContactInfo = personService.getLastFirstName(person);
            currentProvider = providerService.getProviderByPerson(person);
        }
    }

    private boolean findPatientByPatientNumber(String patientNumber, List<Patient> patientList) {
        PatientIdentityService patientIdentityService = SpringContext.getBean(PatientIdentityService.class);
        patientList.addAll(patientService.getPatientsByNationalId(patientNumber));

        if (patientList.isEmpty()) {
            List<PatientIdentity> identities = patientIdentityService.getPatientIdentitiesByValueAndType(patientNumber,
                    PatientServiceImpl.getPatientSTIdentity());

            if (identities.isEmpty()) {
                identities = patientIdentityService.getPatientIdentitiesByValueAndType(patientNumber,
                        PatientServiceImpl.getPatientSubjectIdentity());
            }

            if (!identities.isEmpty()) {

                for (PatientIdentity patientIdentity : identities) {
                    String reportPatientId = patientIdentity.getPatientId();
                    Patient patient = new Patient();
                    patient.setId(reportPatientId);
                    patientService.getData(patient);
                    patientList.add(patient);
                }
            }
        }

        return !patientList.isEmpty();
    }

    private List<Sample> findReportSamplesForReportPatient(List<Patient> patientList) {
        List<Sample> sampleList = new ArrayList<>();
        for (Patient searchPatient : patientList) {
            sampleList.addAll(sampleHumanService.getSamplesForPatient(searchPatient.getId()));
        }

        return sampleList;
    }

    private boolean validateAccessionNumbers() {

        if (GenericValidator.isBlankOrNull(lowerNumber) && GenericValidator.isBlankOrNull(upperNumber)) {
            add1LineErrorMessage("report.error.message.noParameters");
            return false;
        }

        if (GenericValidator.isBlankOrNull(lowerNumber)) {
            lowerNumber = upperNumber;
        } else if (GenericValidator.isBlankOrNull(upperNumber)) {
            upperNumber = lowerNumber;
        }

        if (lowerNumber.length() != upperNumber.length()
                || AccessionNumberUtil.correctFormat(lowerNumber,
                        false) != IAccessionNumberValidator.ValidationResults.SUCCESS
                || AccessionNumberUtil.correctFormat(lowerNumber,
                        false) != IAccessionNumberValidator.ValidationResults.SUCCESS) {
            add1LineErrorMessage("report.error.message.accession.not.valid");
            return false;
        }

        if (lowerNumber.compareToIgnoreCase(upperNumber) > 0) {
            String temp = upperNumber;
            upperNumber = lowerNumber;
            lowerNumber = temp;
        }

        return true;
    }

    private List<Sample> findReportSamples(String lowerNumber, String upperNumber) {
        List<Sample> sampleList = sampleService.getSamplesByAccessionRange(lowerNumber, upperNumber);
        return sampleList == null ? new ArrayList<>() : sampleList;
    }

    private List<Sample> findReportSamples(List<String> analysisIds) {
        List<Sample> sampleList = sampleService.getSamplesByAnalysisIds(analysisIds);
        return sampleList == null ? new ArrayList<>() : sampleList;
    }

    protected void findPatientFromSample() {
        Patient patient = sampleHumanService.getPatientForSample(currentSample);

        if (currentPatient == null || !patient.getId().equals(patientService.getPatientId(currentPatient))) {
            STNumber = null;
            patientDOB = null;
            patientService = SpringContext.getBean(PatientService.class);
            personService.getData(patient.getPerson());
            currentPatient = patient;
        }
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("siteId", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        reportParameters.put("headerName", getHeaderName());
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")) {
            reportParameters.put("useSTNumber", Boolean.FALSE);
        } else {
            reportParameters.put("useSTNumber", Boolean.TRUE);
        }
        if (ConfigurationProperties.getInstance().isCaseInsensitivePropertyValueEqual(Property.configurationName,
                "CI LNSP")
                || ConfigurationProperties.getInstance().isCaseInsensitivePropertyValueEqual(Property.configurationName,
                        "CI IPCI")
                || ConfigurationProperties.getInstance().isCaseInsensitivePropertyValueEqual(Property.configurationName,
                        "CI_REGIONAL")
                || ConfigurationProperties.getInstance().isCaseInsensitivePropertyValueEqual(Property.configurationName,
                        "RETROCI")
                || ConfigurationProperties.getInstance().isCaseInsensitivePropertyValueEqual(Property.configurationName,
                        "CI_GENERAL")) {
            reportParameters.put("useBillingNumber", Boolean.TRUE);
        } else {
            reportParameters.put("useBillingNumber", Boolean.FALSE);
        }
        if (Boolean.valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.CONTACT_TRACING))) {
            reportParameters.put("useContactTracing", Boolean.TRUE);
        } else {
            reportParameters.put("useContactTracing", Boolean.FALSE);
        }
    }

    protected abstract String getHeaderName();

    protected String getPatientDOB(Patient patient) {
        if (patientDOB == null) {
            patientDOB = patientService.getBirthdayForDisplay(patient);
        }

        return patientDOB;
    }

    protected String getLazyPatientIdentity(Patient patient, String identity, String id) {
        if (identity == null) {
            identity = " ";
            List<PatientIdentity> identities = patientService.getIdentityList(patient);
            for (PatientIdentity patientIdentity : identities) {
                if (patientIdentity.getIdentityTypeId().equals(id)) {
                    identity = patientIdentity.getIdentityData();
                    break;
                }
            }
        }

        return identity;
    }

    protected void setPatientName(ClinicalPatientData data) {
        data.setPatientName(patientService.getLastFirstName(currentPatient));
        data.setFirstName(patientService.getFirstName(currentPatient));
        data.setLastName(patientService.getLastName(currentPatient));
    }

    protected void reportResultAndConclusion(ClinicalPatientData data) {
        List<Result> resultList = analysisService.getResults(currentAnalysis);

        Test test = analysisService.getTest(currentAnalysis);
        NoteService noteService = SpringContext.getBean(NoteService.class);
        String note = noteService.getNotesAsString(currentAnalysis, true, true, "<br/>", FILTER, true);
        if (note != null) {
            data.setNote(note);
        }
        data.setTestSection(analysisService.getTestSection(currentAnalysis).getLocalizedName());
        data.setTestSortOrder(GenericValidator.isBlankOrNull(test.getSortOrder()) ? Integer.MAX_VALUE
                : Integer.parseInt(test.getSortOrder()));
        data.setSectionSortOrder(test.getTestSection().getSortOrderInt());

        if (SpringContext.getBean(IStatusService.class).matches(analysisService.getStatusId(currentAnalysis),
                AnalysisStatus.Canceled)) {
            data.setResult(MessageUtil.getMessage("report.test.status.canceled"));
        } else if (currentAnalysis.isReferredOut()) {
            setReferredOutResult(data);
            return;
            /*
             * Not sure which rules this would support -- above statement was conditional on
             * no patient alerts if( noResults( resultList ) ){ data.setResult(
             * MessageUtil.getMessage( "report.test.status.referredOut" ) ); }else{
             * setAppropriateResults( resultList, data ); setReferredResult( data,
             * resultList.get( 0 ) ); setNormalRange( data, test, resultList.get( 0 ) ); }
             */
        } else if (!SpringContext.getBean(IStatusService.class).matches(analysisService.getStatusId(currentAnalysis),
                AnalysisStatus.Finalized)
                && !(SpringContext.getBean(IStatusService.class).matches(analysisService.getStatusId(currentAnalysis),
                        AnalysisStatus.TechnicalRejected)
                        && ConfigurationProperties.getInstance().isPropertyValueEqual(
                                ConfigurationProperties.Property.VALIDATE_REJECTED_TESTS, "false"))) {

            if (sampleCompleteMap != null) {
                sampleCompleteMap.put(convertToAlphaNumericDisplay(currentSample), Boolean.FALSE);
            }
            setEmptyResult(data);
        } else {
            if (resultList.isEmpty()) {
                setEmptyResult(data);
            } else {
                setAppropriateResults(resultList, data);
                Result result = resultList.get(0);
                setCorrectedStatus(result, data);
                setNormalRange(data, test, result);
                data.setResult(getAugmentedResult(data, result));
                data.setFinishDate(analysisService.getCompletedDateForDisplay(currentAnalysis));
                data.setAlerts(getResultFlag(result, null, data));
            }
        }

        data.setParentResult(currentAnalysis.getParentResult());
        data.setConclusion(currentConclusion);
    }

    protected void setReferredOutResult(ClinicalPatientData data) {
        data.setResult(MessageUtil.getMessage("report.test.status.inProgress"));
    }

    protected void setEmptyResult(ClinicalPatientData data) {
        data.setResult(MessageUtil.getMessage("report.test.status.inProgress"));
    }

    private void setCorrectedStatus(Result result, ClinicalPatientData data) {
        if (currentAnalysis.isCorrectedSincePatientReport() && !GenericValidator.isBlankOrNull(result.getValue())) {
            data.setCorrectedResult(true);
            data.setContactInfo(currentContactInfo);
            sampleCorrectedMap.put(convertToAlphaNumericDisplay(currentSample), true);
            currentAnalysis.setCorrectedSincePatientReport(false);
            updatedAnalysis.add(currentAnalysis);
        }
    }

    private void setNormalRange(ClinicalPatientData data, Test test, Result result) {
        String uom = getUnitOfMeasure(test);
        data.setTestRefRange(addIfNotEmpty(getRange(result), appendUOMToRange() ? uom : null));
        data.setUom(uom);
    }

    private String getAugmentedResult(ClinicalPatientData data, Result result) {
        String resultValue = data.getResult();
        if (TestIdentityService.getInstance().isTestNumericViralLoad(analysisService.getTest(currentAnalysis))) {
            try {
                resultValue += " (" + formatTwoDecimals(Math.log10(Double.parseDouble(resultValue))) + ")log ";
            } catch (IllegalFormatException e) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "getAugmentedResult", e.getMessage());
                // no-op
            }
        }

        return resultValue + (augmentResultWithFlag() ? getResultFlag(result, null) : "");
    }

    // thread safe implementation
    protected synchronized String formatTwoDecimals(Double value) {
        return twoDecimalFormat.format(value);
    }

    protected String getResultFlag(Result result, String imbed) {
        return getResultFlag(result, imbed, null);
    }

    protected String getResultFlag(Result result, String imbed, ClinicalPatientData data) {
        String flag = "";
        try {
            if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(result.getResultType())
                    && !GenericValidator.isBlankOrNull(result.getValue())) {
                if (result.getMinNormal() != null & result.getMaxNormal() != null
                        && (result.getMinNormal() != 0.0 || result.getMaxNormal() != 0.0)) {
                    if (Double.valueOf(result.getValue(true)) < result.getMinNormal()) {
                        flag = "B";
                    } else if (Double.valueOf(result.getValue(true)) > result.getMaxNormal()) {
                        flag = "E";
                    }
                }
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
                boolean isAbnormal;

                if (data == null) {
                    ResultService resultResultService = SpringContext.getBean(ResultService.class);
                    isAbnormal = resultResultService.isAbnormalDictionaryResult(result);
                } else {
                    isAbnormal = data.getAbnormalResult();
                }
                if (isAbnormal) {
                    flag = "*";
                }
            }

            if (!GenericValidator.isBlankOrNull(flag)) {
                if (data != null) {
                    data.setAbnormalResult(Boolean.TRUE);
                }
                if (!GenericValidator.isBlankOrNull(imbed)) {
                    return " <b>" + flag + "," + imbed + "</b>";
                } else {
                    return " <b>" + flag + "</b>";
                }
            }
        } catch (NumberFormatException e) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getResultFlag", e.getMessage());
            // no-op
        }

        if (!GenericValidator.isBlankOrNull(imbed)) {
            return " (<b>" + imbed + "</b>)";
        }

        return "";
    }

    protected String getRange(Result result) {
        ResultService resultResultService = SpringContext.getBean(ResultService.class);
        return resultResultService.getDisplayReferenceRange(result, true);
    }

    protected String getUnitOfMeasure(Test test) {
        return (test != null && test.getUnitOfMeasure() != null) ? test.getUnitOfMeasure().getName() : "";
    }

    private void setAppropriateResults(List<Result> resultList, ClinicalPatientData data) {
        String reportResult = "";
        if (!resultList.isEmpty()) {

            // If only one result just get it and get out
            if (resultList.size() == 1) {
                Result result = resultList.get(0);
                if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
                    Dictionary dictionary = new Dictionary();
                    dictionary.setId(result.getValue());
                    dictionaryService.getData(dictionary);
                    ResultService resultResultService = SpringContext.getBean(ResultService.class);
                    data.setAbnormalResult(resultResultService.isAbnormalDictionaryResult(result));

                    if (result.getAnalyte() != null && "Conclusion".equals(result.getAnalyte().getAnalyteName())) {
                        currentConclusion = dictionary.getId() != null ? dictionary.getLocalizedName() : "";
                    } else {
                        reportResult = dictionary.getId() != null ? dictionary.getLocalizedName() : "";
                    }
                } else {
                    ResultService resultResultService = SpringContext.getBean(ResultService.class);
                    reportResult = resultResultService.getResultValue(result, true);
                    // TODO - how is this used. Selection types can also have
                    // UOM and reference ranges
                    data.setHasRangeAndUOM(
                            TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(result.getResultType()));
                }
            } else {
                // If multiple results it can be a quantified result, multiple
                // results with quantified other results or it can be a
                // conclusion
                ResultService resultResultService = SpringContext.getBean(ResultService.class);
                Result result = resultList.get(0);

                if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY
                        .matches(resultResultService.getTestType(result))) {
                    data.setAbnormalResult(resultResultService.isAbnormalDictionaryResult(result));
                    List<Result> dictionaryResults = new ArrayList<>();
                    Result quantification = null;
                    for (Result sibResult : resultList) {
                        if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(sibResult.getResultType())) {
                            dictionaryResults.add(sibResult);
                        } else if (TypeOfTestResultServiceImpl.ResultType.ALPHA.matches(sibResult.getResultType())
                                && sibResult.getParentResult() != null) {
                            quantification = sibResult;
                        }
                    }

                    Dictionary dictionary = new Dictionary();
                    for (Result sibResult : dictionaryResults) {
                        dictionary.setId(sibResult.getValue());
                        dictionaryService.getData(dictionary);
                        if (sibResult.getAnalyte() != null
                                && "Conclusion".equals(sibResult.getAnalyte().getAnalyteName())) {
                            currentConclusion = dictionary.getId() != null ? dictionary.getLocalizedName() : "";
                        } else {
                            reportResult = dictionary.getId() != null ? dictionary.getLocalizedName() : "";
                            if (quantification != null
                                    && quantification.getParentResult().getId().equals(sibResult.getId())) {
                                reportResult += ": " + quantification.getValue();
                            }
                        }
                    }
                } else if (TypeOfTestResultServiceImpl.ResultType
                        .isMultiSelectVariant(resultResultService.getTestType(result))) {
                    Dictionary dictionary = new Dictionary();
                    StringBuilder multiResult = new StringBuilder();

                    Collections.sort(resultList, new Comparator<Result>() {
                        @Override
                        public int compare(Result o1, Result o2) {
                            if (o1.getGrouping() == o2.getGrouping()) {
                                return Integer.parseInt(o1.getId()) - Integer.parseInt(o2.getId());
                            } else {
                                return o1.getGrouping() - o2.getGrouping();
                            }
                        }
                    });

                    Result quantifiedResult = null;
                    for (Result subResult : resultList) {
                        if (TypeOfTestResultServiceImpl.ResultType.ALPHA.matches(subResult.getResultType())) {
                            quantifiedResult = subResult;
                            resultList.remove(subResult);
                            break;
                        }
                    }
                    int currentGrouping = resultList.get(0).getGrouping();
                    for (Result subResult : resultList) {
                        if (subResult.getGrouping() != currentGrouping) {
                            currentGrouping = subResult.getGrouping();
                            multiResult.append("-------\n");
                        }
                        dictionary.setId(subResult.getValue());
                        dictionaryService.getData(dictionary);

                        if (dictionary.getId() != null) {
                            multiResult.append(dictionary.getLocalizedName());
                            if (quantifiedResult != null
                                    && quantifiedResult.getParentResult().getId().equals(subResult.getId())
                                    && !GenericValidator.isBlankOrNull(quantifiedResult.getValue())) {
                                multiResult.append(": ");
                                multiResult.append(quantifiedResult.getValue());
                            }
                            multiResult.append("\n");
                        }
                    }

                    if (multiResult.length() > 1) {
                        // remove last "\n"
                        multiResult.setLength(multiResult.length() - 1);
                    }

                    reportResult = multiResult.toString();
                }
            }
        }
        data.setResult(reportResult);
    }

    protected void setCollectionTime(Set<SampleItem> sampleSet, List<ClinicalPatientData> currentSampleReportItems,
            boolean addAccessionNumber) {
        StringBuilder buffer = new StringBuilder();
        boolean firstItem = true;
        for (SampleItem sampleItem : sampleSet) {
            if (firstItem) {
                firstItem = false;
            } else {
                buffer.append(", ");
            }

            buffer.append(sampleItem.getTypeOfSample().getLocalizedName());
            if (addAccessionNumber) {
                buffer.append(" ");
                buffer.append(sampleItem.getSample().getAccessionNumber() + "-" + sampleItem.getSortOrder());
            }
            if (sampleItem.getCollectionDate() == null) {
                buffer.append(" -- ");
                buffer.append(MessageUtil.getMessage("label.not.available"));
            } else {
                buffer.append(" ");
                buffer.append(
                        DateUtil.convertTimestampToStringDateAndConfiguredHourTime(sampleItem.getCollectionDate()));
            }
        }

        String collectionTimes = buffer.toString();
        for (ClinicalPatientData clinicalPatientData : currentSampleReportItems) {
            clinicalPatientData.setCollectionDateTime(collectionTimes);
        }
    }

    /**
     * @see PatientReport#initializeReportItems()
     */
    protected void initializeReportItems() {
        reportItems = new ArrayList<>();
    }

    /**
     * If you have a string that you wish to add a suffix like units of measure, use
     * this.
     *
     * @param base something
     * @param plus something to add, if the above is not null or blank.
     * @return the two args put together, or the original if it was blank to begin
     *         with.
     */
    protected String addIfNotEmpty(String base, String plus) {
        return (!GenericValidator.isBlankOrNull(plus)) ? base + " " + plus : base;
    }

    /**
     * Pushes all of the information about a patient, analysis, result and the
     * conclusion into a new reporting object
     *
     * @return A single record
     */
    protected ClinicalPatientData buildClinicalPatientData(boolean hasParent) {
        ClinicalPatientData data = new ClinicalPatientData();
        String testName = null;
        String sortOrder = "";
        String receivedDate = sampleService.getReceivedDateForDisplay(currentSample);

        boolean doAnalysis = analysisService != null;

        if (doAnalysis) {
            testName = getTestName(hasParent);
            // Not sure if it is a bug in escapeHtml but the wrong markup is
            // generated
            testName = StringEscapeUtils.escapeHtml4(testName).replace("&mu", "&micro");
        }

        if (FormFields.getInstance().useField(Field.SampleEntryUseReceptionHour)) {
            receivedDate += " " + sampleService.getReceivedTimeForDisplay(currentSample);
        }
        ObservationHistoryService observationHistoryService = SpringContext.getBean(ObservationHistoryService.class);

        data.setContactInfo(currentContactInfo);
        data.setSiteInfo(currentSiteInfo);
        data.setReceivedDate(receivedDate);
        data.setDob(getPatientDOB(currentPatient));
        data.setAge(createReadableAge(data.getDob()));
        data.setGender(patientService.getGender(currentPatient));
        data.setNationalId(patientService.getNationalId(currentPatient));
        setPatientName(data);
        data.setDept(patientDept);
        data.setCommune(patientCommune);
        data.setStNumber(getLazyPatientIdentity(currentPatient, STNumber, PatientServiceImpl.getPatientSTIdentity()));
        data.setSubjectNumber(
                getLazyPatientIdentity(currentPatient, subjectNumber, PatientServiceImpl.getPatientSubjectIdentity()));
        data.setHealthRegion(getLazyPatientIdentity(currentPatient, healthRegion,
                PatientServiceImpl.getPatientHealthRegionIdentity()));
        data.setHealthDistrict(getLazyPatientIdentity(currentPatient, healthDistrict,
                PatientServiceImpl.getPatientHealthDistrictIdentity()));

        data.setLabOrderType(observationHistoryService.getValueForSample(ObservationType.PROGRAM,
                sampleService.getId(currentSample)));
        data.setTestName(testName);
        data.setPatientSiteNumber(observationHistoryService.getValueForSample(ObservationType.REFERRERS_PATIENT_ID,
                sampleService.getId(currentSample)));
        data.setBillingNumber(observationHistoryService.getValueForSample(ObservationType.BILLING_REFERENCE_NUMBER,
                sampleService.getId(currentSample)));

        if (doAnalysis) {
            data.setPanel(analysisService.getPanel(currentAnalysis));
            if (analysisService.getPanel(currentAnalysis) != null) {
                data.setPanelName(analysisService.getPanel(currentAnalysis).getLocalizedName());
            }
            data.setTestDate(analysisService.getCompletedDateForDisplay(currentAnalysis));
            data.setSampleSortOrder(currentAnalysis.getSampleItem().getSortOrder());
            data.setOrderFinishDate(completionDate);
            data.setOrderDate(DateUtil
                    .convertTimestampToStringDateAndConfiguredHourTime(sampleService.getOrderedDate(currentSample)));
            data.setSampleId(sampleService.getAccessionNumber(currentSample) + "-" + data.getSampleSortOrder());
            data.setSampleType(analysisService.getTypeOfSample(currentAnalysis).getLocalizedName());
            data.setCollectionDateTime(DateUtil.convertTimestampToStringDateAndConfiguredHourTime(
                    currentAnalysis.getSampleItem().getCollectionDate()));
        }
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            if (doAnalysis) {
                data.setSampleId(AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(
                        sampleService.getAccessionNumber(currentSample)) + "-" + data.getSampleSortOrder());
            }
            data.setAccessionNumber(AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(
                    sampleService.getAccessionNumber(currentSample)) + "-" + data.getSampleSortOrder());
        } else {
            if (doAnalysis) {
                data.setSampleId(sampleService.getAccessionNumber(currentSample) + "-" + data.getSampleSortOrder());
            }
            data.setAccessionNumber(sampleService.getAccessionNumber(currentSample) + "-" + data.getSampleSortOrder());
        }

        if (doAnalysis) {
            reportResultAndConclusion(data);
        }
        if (Boolean.valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.CONTACT_TRACING))) {
            data.setContactTracingIndexName(
                    sampleService.getSampleAdditionalFieldForSample(sampleService.getId(currentSample),
                            AdditionalFieldName.CONTACT_TRACING_INDEX_NAME).getFieldValue());
            data.setContactTracingIndexRecordNumber(
                    sampleService.getSampleAdditionalFieldForSample(sampleService.getId(currentSample),
                            AdditionalFieldName.CONTACT_TRACING_INDEX_RECORD_NUMBER).getFieldValue());
        }
        String testSection = analysisService.getTestSection(currentAnalysis).getDescription();
        if (testSection.equals("Tuberculose")) {
            data.setTbOrderReason(observationHistoryService.getValueForSample(ObservationType.TB_ORDER_REASON,
                    sampleService.getId(currentSample)));
            data.setTbDiagnosticReason(observationHistoryService.getValueForSample(ObservationType.TB_DIAGNOSTIC_REASON,
                    sampleService.getId(currentSample)));
            data.setTbFollowupReason(observationHistoryService.getValueForSample(ObservationType.TB_FOLLOWUP_REASON,
                    sampleService.getId(currentSample)));
            data.setTbAnalysisMethod(observationHistoryService.getValueForSample(ObservationType.TB_ANALYSIS_METHOD,
                    sampleService.getId(currentSample)));
            data.setTbSampleAspect(observationHistoryService.getValueForSample(ObservationType.TB_SAMPLE_ASPECT,
                    sampleService.getId(currentSample)));
            data.setTbFollowupPeriodLine1(observationHistoryService
                    .getValueForSample(ObservationType.TB_FOLLOWUP_PERIOD_LINE1, sampleService.getId(currentSample)));
            data.setTbFollowupPeriodLine2(observationHistoryService
                    .getValueForSample(ObservationType.TB_FOLLOWUP_PERIOD_LINE2, sampleService.getId(currentSample)));
        }

        return data;
    }

    private String getTestName(boolean indent) {
        String testName;

        if (useReportingDescription()) {
            testName = TestServiceImpl.getUserLocalizedReportingTestName(analysisService.getTest(currentAnalysis));
        } else {
            testName = TestServiceImpl.getUserLocalizedTestName(analysisService.getTest(currentAnalysis));
        }

        if (GenericValidator.isBlankOrNull(testName)) {
            testName = TestServiceImpl.getUserLocalizedTestName(analysisService.getTest(currentAnalysis));
        }
        return (indent ? "    " : "") + testName;
    }

    /**
     * Given a list of referralResults for a particular analysis, generated the next
     * displayable value made of from one or more of the values from the list
     * starting at the given index. It uses multiresult form the list when the
     * results are for the same test.
     *
     * @param referralResultsForReferral The referral
     * @param i                          starting index.
     * @return last index actually used. If you start with 2 and this routine uses
     *         just item #2, then return result is 2, but if there are two results
     *         for the same test (e.g. a multi-select result) and those are in item
     *         item 2 and item 3 this routine returns #3.
     */
    protected int lastUsedReportReferralResultValue(List<ReferralResult> referralResultsForReferral, int i) {
        ReferralResult referralResult = referralResultsForReferral.get(i);
        reportReferralResultValue = "";
        String currTestId = referralResult.getTestId();
        do {
            reportReferralResultValue += findDisplayableReportResult(referralResult.getResult()) + ", ";
            i++;
            if (i >= referralResultsForReferral.size()) {
                break;
            }
            referralResult = referralResultsForReferral.get(i);
        } while (currTestId.equals(referralResult.getTestId()));
        reportReferralResultValue = reportReferralResultValue.substring(0, reportReferralResultValue.length() - 2);
        i--;
        return i;
    }

    /**
     * Derive the appropriate displayable string results, either dictionary result
     * or direct value.
     *
     * @param result The result
     * @return a reportable result string.
     */
    private String findDisplayableReportResult(Result result) {
        String reportResult = "";
        if (result == null) {
            return reportResult;
        }
        String type = result.getResultType();
        String value = result.getValue();
        if (value == null) {
            return reportResult;
        }
        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(type)) {
            if (result.getValue() != null && !"null".equals(result.getValue())) {
                Dictionary dictionary = new Dictionary();
                dictionary.setId(result.getValue());
                dictionaryService.getData(dictionary);
                reportResult = dictionary.getId() != null ? dictionary.getLocalizedName() : "";
            }
        } else {
            reportResult = result.getValue();
        }
        return reportResult;
    }

    private String createReadableAge(String dob) {
        if (GenericValidator.isBlankOrNull(dob)) {
            return "";
        }

        dob = dob.replaceAll(DateUtil.AMBIGUOUS_DATE_SEGMENT, "01");
        Date dobDate = DateUtil.convertStringDateToSqlDate(dob);
        int months = DateUtil.getAgeInMonths(dobDate, DateUtil.getNowAsSqlDate());
        if (months > 35) {
            return (months / 12) + " " + MessageUtil.getMessage("abbreviation.year.single");
        } else if (months > 0) {
            return months + " " + MessageUtil.getMessage("abbreviation.month.single");
        } else {
            int days = DateUtil.getAgeInDays(dobDate, DateUtil.getNowAsSqlDate());
            return days + " " + MessageUtil.getMessage("abbreviation.day.single");
        }
    }

    @Override
    public List<String> getReportedOrders() {
        return handledOrders;
    }
}
