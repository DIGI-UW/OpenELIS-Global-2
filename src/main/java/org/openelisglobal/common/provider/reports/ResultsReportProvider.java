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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.common.provider.reports;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.util.JRLoader;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSResultsReportHasNoDataException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.reports.valueholder.common.JRHibernateDataSource;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportAnalyteResult;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportAnalyteResultComparator;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportLabProject;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportSample;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportSampleComparator;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportTest;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.openelisglobal.sourceofsample.valueholder.SourceOfSample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;

/**
 * @author benzd1 bugzilla 2264 bugzilla 1856 move pending tests to top section
 *         sort tests (parent/child recursive by sort order of test) add section
 *         previously reported tests
 */
// TODO csl - this class is the only one that uses TransactionTemplate,
// this should be changed so it uses @Transactional  for transaction management like every other
// class
public class ResultsReportProvider extends BaseReportsProvider {

    private final TransactionTemplate transactionTemplate;

    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    protected SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected ResultService resultService = SpringContext.getBean(ResultService.class);
    protected DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);

    protected PatientService patientService = SpringContext.getBean(PatientService.class);
    protected PersonService personService = SpringContext.getBean(PersonService.class);
    protected ProviderService providerService = SpringContext.getBean(ProviderService.class);
    protected SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
    protected SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    protected SampleOrganizationService sampleOrganizationService = SpringContext
            .getBean(SampleOrganizationService.class);

    private String dateAsText;
    private String originalMessage;
    private String amendedMessage;
    private String typeMessage;
    private String sourceMessage;
    private String sourceOtherMessage;
    private String notApplicableMessage;
    String testingPendingMessage;
    private String resultsReportType;
    private ResultsReportAnalyteResult reportAnalyteResult;
    private static final int CURRENT_SECTION = 1;
    private static final int PREVIOUS_SECTION = 2;

    public ResultsReportProvider() {
        PlatformTransactionManager transactionManager = SpringContext.getBean(PlatformTransactionManager.class);
        Assert.notNull(transactionManager, "transactionManager must not be null");
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.provider.reports.BaseReportsProvider#
     * processRequest(java.util.Map, jakarta.servlet.http.HttpServletRequest,
     * jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    public boolean processRequest(Map parameters, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // either standard or amended
        if (request.getParameter(RESULTS_REPORT_TYPE_PARAM) != null) {
            resultsReportType = request.getParameter(RESULTS_REPORT_TYPE_PARAM);
        } else {
            // bugzilla 1900 preview
            resultsReportType = (String) request.getAttribute(RESULTS_REPORT_TYPE_PARAM);
        }

        // bugzilla 1900 for preview we may have one to many accession numbers (samples)
        // for which we need to preview report
        final List accessionNumbers = new ArrayList();
        if (resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)) {
            if (request.getAttribute(ACCESSION_NUMBERS) != null) {
                accessionNumbers.addAll((ArrayList) request.getAttribute(ACCESSION_NUMBERS));
            }
        }

        ClassLoader classLoader = getClass().getClassLoader();
        File mainReportFile = new File(classLoader.getResource("/reports/rslts_main_report.jasper").getFile());
        // File mainReportFile = new File(contexct
        // .getRealPath("/WEB-INF/reports/rslts_main_report.jasper").getFile());

        // this report has several sub reports (and other dependencies)
        // it is recommended to pass these in as JasperReport object parameters
        // this works both for oc4j AND tomcat (where problems were occuring before this
        // mod)
        File providerDetailsReportFile = new File(
                classLoader.getResource("/reports/rslts_provider_details.jasper").getFile());
        File projectDetailsReportFile = new File(
                classLoader.getResource("/reports/rslts_project_details.jasper").getFile());
        File sourceTypeDetailsReportFile = new File(
                classLoader.getResource("/reports/rslts_sourcetype_details.jasper").getFile());
        File patientDetailsReportFile = new File(
                classLoader.getResource("/reports/rslts_patient_details.jasper").getFile());
        File testResultsReportFile = new File(classLoader.getResource("/reports/rslts_test_results.jasper").getFile());
        File resultValueReportFile = new File(classLoader.getResource("/reports/rslts_result_value.jasper").getFile());
        File logoGifFile = new File(classLoader.getResource("/reports/images/rslts_logo.gif").getFile());
        // bugzilla 1900
        File previewWaterMark = new File(
                classLoader.getResource("/reports/images/rslts_previewwatermark.gif").getFile());

        Locale locale = LocaleContextHolder.getLocale();
        // bugzilla 2227
        Date today = Calendar.getInstance().getTime();
        dateAsText = DateUtil.formatDateAsText(today);

        // doInTransaction used instead of beginTransaction
        // TODO csl - refactor class to use @Transactional transaction management
        // would involve extracting this block to a new method annotated with
        // @Transactional in a @Service class wired in as an interface
        return transactionTemplate.execute(new TransactionCallback<Boolean>() {

            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                Errors errors = new BaseErrors();
                try {
                    parameters.put(JRParameter.REPORT_LOCALE, locale);
                    parameters.put(JRParameter.REPORT_RESOURCE_BUNDLE,
                            ResourceBundle.getBundle("languages/message", locale));

                    // turn subreport jasper files into JasperReport objects to pass in as
                    // parameters
                    JasperReport providerDetailsReport = (JasperReport) JRLoader
                            .loadObjectFromFile(providerDetailsReportFile.getPath());
                    JasperReport projectDetailsReport = (JasperReport) JRLoader
                            .loadObjectFromFile(projectDetailsReportFile.getPath());
                    JasperReport sourceTypeDetailsReport = (JasperReport) JRLoader
                            .loadObjectFromFile(sourceTypeDetailsReportFile.getPath());
                    JasperReport patientDetailsReport = (JasperReport) JRLoader
                            .loadObjectFromFile(patientDetailsReportFile.getPath());
                    JasperReport testResultsReport = (JasperReport) JRLoader
                            .loadObjectFromFile(testResultsReportFile.getPath());
                    JasperReport resultValueReport = (JasperReport) JRLoader
                            .loadObjectFromFile(resultValueReportFile.getPath());

                    parameters.put("Provider_Details", providerDetailsReport);
                    parameters.put("Project_Details", projectDetailsReport);
                    parameters.put("Sourcetype_Details", sourceTypeDetailsReport);
                    parameters.put("Patient_Details", patientDetailsReport);
                    parameters.put("Test_Results", testResultsReport);
                    parameters.put("Result_Value", resultValueReport);
                    parameters.put("Logo", logoGifFile);
                    // bugzilla 1900
                    parameters.put("PreviewWaterMark", previewWaterMark);

                    // bugzilla 2227 find out if there are amended results
                    List unfilteredListOfCurrentAnalyses = new ArrayList();
                    // bugzilla 1900
                    if (resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)) {
                        unfilteredListOfCurrentAnalyses = analysisService
                                .getMaxRevisionAnalysesReadyForReportPreviewBySample(accessionNumbers);
                    } else {
                        unfilteredListOfCurrentAnalyses = analysisService.getMaxRevisionAnalysesReadyToBeReported();
                    }

                    List analysesPrinted = new ArrayList();

                    HashMap samples = new HashMap();
                    ResultsReportSample reportSample = new ResultsReportSample();
                    List currentTests = new ArrayList();
                    // List previousTests = new ArrayList();
                    List currentTestsToReport = new ArrayList();
                    // List previousTestsToReport = new ArrayList();
                    // List currentAnalyteResults = new ArrayList();
                    // List previousAnalyteResults = new ArrayList();
                    reportAnalyteResult = new ResultsReportAnalyteResult();

                    amendedMessage = getMessageForKey(request, "label.jasper.results.report.amended");
                    originalMessage = getMessageForKey(request, "label.jasper.results.report.original");
                    sourceMessage = getMessageForKey(request, "label.jasper.results.report.sourceofsample");
                    typeMessage = getMessageForKey(request, "label.jasper.results.report.typeofsample");
                    sourceOtherMessage = getMessageForKey(request, "label.jasper.results.report.sourceother");
                    notApplicableMessage = getMessageForKey(request, "label.jasper.results.report.notapplicable");
                    testingPendingMessage = getMessageForKey(request, "label.jasper.results.report.testing.pending");

                    currentTestsToReport = populateTests(unfilteredListOfCurrentAnalyses, CURRENT_SECTION);

                    // bugzilla 1900
                    if (resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)
                            && (currentTestsToReport == null || currentTestsToReport.size() == 0)) {
                        // throw Exception
                        throw new LIMSResultsReportHasNoDataException("No data to display");
                    }

                    // based on currentTestsToReport find and populate sample information for
                    // reporting
                    for (int i = 0; i < currentTestsToReport.size(); i++) {
                        ResultsReportTest reportTest = (ResultsReportTest) currentTestsToReport.get(i);
                        String id = reportTest.getAnalysisId();
                        Analysis analysis = new Analysis();
                        analysis.setId(id);
                        analysisService.getData(analysis);

                        String accessionNumber = analysis.getSampleItem().getSample().getAccessionNumber();
                        if (samples.containsKey(accessionNumber)) {
                            // get existing sample object
                            reportSample = (ResultsReportSample) samples.get(accessionNumber);
                            if (!analysis.getRevision().equals("0")) {
                                reportSample.setSampleHasTestRevisions(TRUE);
                            }
                            currentTests = reportSample.getTests();
                        } else {
                            Patient patient = new Patient();
                            Person person = new Person();
                            Provider provider = new Provider();
                            Person providerPerson = new Person();
                            Sample sample = new Sample();
                            SampleHuman sampleHuman = new SampleHuman();
                            SampleOrganization sampleOrganization = new SampleOrganization();
                            SampleItem sampleItem = new SampleItem();
                            TypeOfSample typeOfSample = new TypeOfSample();
                            SourceOfSample sourceOfSample = new SourceOfSample();
                            String sourceOther = null;

                            sample.setAccessionNumber(accessionNumber);
                            sampleService.getSampleByAccessionNumber(sample);
                            if (!StringUtil.isNullorNill(sample.getId())) {
                                reportSample = new ResultsReportSample();
                                // initialize this value to false
                                reportSample.setSampleHasTestRevisions(FALSE);
                                // bugzilla 1900
                                if (resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)) {
                                    reportSample.setSampleIsForPreview(TRUE);
                                }

                                reportSample.setSample(analysis.getSampleItem().getSample());

                                sampleHuman.setSampleId(sample.getId());
                                sampleHumanService.getDataBySample(sampleHuman);
                                sampleOrganization.setSample(sample);
                                sampleOrganizationService.getDataBySample(sampleOrganization);
                                sampleItem.setSample(sample);
                                sampleItemService.getDataBySample(sampleItem);
                                patient.setId(sampleHuman.getPatientId());
                                patientService.getData(patient);
                                person = patient.getPerson();
                                personService.getData(person);

                                provider.setId(sampleHuman.getProviderId());
                                providerService.getData(provider);
                                providerPerson = provider.getPerson();
                                personService.getData(providerPerson);

                                sourceOfSample = sampleItem.getSourceOfSample();
                                typeOfSample = sampleItem.getTypeOfSample();
                                sourceOther = sampleItem.getSourceOther();

                                // now populate the reportSample
                                Organization organization = sampleOrganization.getOrganization();
                                String patientName = " " + StringUtil.trim(person.getLastName()) + ", "
                                        + StringUtil.trim(person.getFirstName()) + "  "
                                        + StringUtil.trim(person.getMiddleName());
                                reportSample.setPatientName(patientName);
                                String patientStreetAddress = " " + StringUtil.trim(person.getStreetAddress()) + "  "
                                        + StringUtil.trim(person.getMultipleUnit());
                                reportSample.setPatientStreetAddress(patientStreetAddress);
                                // bugzilla 1852 splip out individual patient address fields
                                String patientCity = "" + StringUtil.trim(person.getCity());
                                String patientState = "" + StringUtil.trim(person.getState());
                                String patientZip = "" + StringUtil.trim(person.getZipCode());
                                String patientCountry = "" + StringUtil.trim(person.getCountry());
                                reportSample.setPatientCity(patientCity);
                                reportSample.setPatientState(patientState);
                                reportSample.setPatientZip(patientZip);
                                reportSample.setPatientCountry(patientCountry);
                                String patientGender = patient.getGender();
                                reportSample.setPatientGender(patientGender);
                                String patientExternalId = patient.getExternalId();
                                reportSample.setPatientExternalId(patientExternalId);
                                String patientDateOfBirth = patient.getBirthDateForDisplay();
                                reportSample.setPatientDateOfBirth(patientDateOfBirth);

                                reportSample.setSampleItem(sampleItem);
                                reportSample.setAccessionNumber(sample.getAccessionNumber());
                                // bugzilla 2633
                                if (organization != null) {
                                    reportSample.setOrganizationId(organization.getId());
                                    reportSample.setOrganizationName(organization.getOrganizationName());
                                    reportSample.setOrganizationStreetAddress(
                                            " " + StringUtil.trim(organization.getStreetAddress()) + " "
                                                    + StringUtil.trim(organization.getMultipleUnit()));
                                    reportSample
                                            .setOrganizationCityStateZip(" " + StringUtil.trim(organization.getCity())
                                                    + " " + StringUtil.trim(organization.getState()) + " "
                                                    + StringUtil.trim(organization.getZipCode()));
                                }
                                reportSample.setSampleCollectionDate(sample.getCollectionDateForDisplay() + " "
                                        + sample.getCollectionTimeForDisplay());
                                reportSample.setSampleReceivedDate(sample.getReceivedDateForDisplay());
                                reportSample.setSampleClientReferenceNumber(sample.getClientReference());
                                reportSample.setClinicianName(" " + StringUtil.trim(providerPerson.getLastName()) + " "
                                        + StringUtil.trim(providerPerson.getFirstName()));

                                SampleProject project1 = null;
                                SampleProject project2 = null;
                                List sampleProjects = sample.getSampleProjects();
                                if (sampleProjects != null && sampleProjects.size() > 0) {
                                    project1 = (SampleProject) sampleProjects.get(0);
                                    if (sampleProjects.size() > 1) {
                                        project2 = (SampleProject) sampleProjects.get(1);
                                    }
                                }

                                ResultsReportLabProject labProject = null;
                                List labProjects = new ArrayList();
                                if (project1 != null) {
                                    // add a labProject object
                                    String project = " " + project1.getProject().getId() + "/"
                                            + project1.getProject().getProjectName();
                                    labProject = new ResultsReportLabProject();
                                    labProject.setLabProject(project);
                                    labProjects.add(labProject);
                                }
                                if (project2 != null) {
                                    // add a labProject object
                                    String project = " " + project2.getProject().getId() + "/"
                                            + project2.getProject().getProjectName();
                                    labProject = new ResultsReportLabProject();
                                    labProject.setLabProject(project);
                                    labProjects.add(labProject);
                                }
                                JRHibernateDataSource resultsReport_Projects = new JRHibernateDataSource(labProjects);
                                reportSample.setResultsReportProjects(resultsReport_Projects);

                                // source of sample
                                if (sampleItem.getSourceOfSample() != null
                                        && !StringUtil.isNullorNill(sampleItem.getSourceOther())) {
                                    reportSample.setSourceOfSample(sourceMessage + ":" + sourceOfSample.getDescription()
                                            + "," + sourceOtherMessage + ":" + sourceOther);
                                } else if (sampleItem.getSourceOfSample() != null
                                        && StringUtil.isNullorNill(sampleItem.getSourceOther())) {
                                    reportSample.setSourceOfSample(sourceMessage + ":" + sourceOfSample.getDescription()
                                            + "," + sourceOtherMessage + ":" + notApplicableMessage);
                                } else if (sampleItem.getSourceOfSample() == null
                                        && !StringUtil.isNullorNill(sampleItem.getSourceOther())) {
                                    reportSample.setSourceOfSample(sourceMessage + ":" + notApplicableMessage + ","
                                            + sourceOtherMessage + ":" + sourceOther);
                                } else if (sampleItem.getSourceOfSample() == null
                                        && StringUtil.isNullorNill(sampleItem.getSourceOther())) {
                                    reportSample.setSourceOfSample(sourceMessage + ":" + notApplicableMessage + ","
                                            + sourceOtherMessage + ":" + notApplicableMessage);
                                }
                                // type of sample
                                reportSample.setTypeOfSample(typeMessage + ":" + typeOfSample.getDescription());

                                if (!analysis.getRevision().equals("0")) {
                                    reportSample.setSampleHasTestRevisions(TRUE);
                                }
                                currentTests = new ArrayList();
                            }
                        }
                        currentTests.add(reportTest);
                        reportSample.setTests(currentTests);
                        samples.put(accessionNumber, reportSample);
                    }

                    // JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(samples);
                    // convert hashmap to list
                    List samplesList = new ArrayList();
                    for (Iterator entryIter = samples.entrySet().iterator(); entryIter.hasNext();) {
                        Map.Entry entry = (Map.Entry) entryIter.next();

                        ResultsReportSample samp = (ResultsReportSample) entry.getValue();

                        // are we printing this sample?
                        if ((samp.getSampleHasTestRevisions().equals(TRUE)
                                && resultsReportType.equals(RESULTS_REPORT_TYPE_AMENDED))
                                || (samp.getSampleHasTestRevisions().equals(FALSE)
                                        && resultsReportType.equals(RESULTS_REPORT_TYPE_ORIGINAL))
                        // bugzilla 1900
                                || (resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW))) {
                            // then this sample will print
                        } else {
                            // skip this sample
                            continue;
                        }

                        // also make JRHibernateDataSource objects out of subreport lists
                        currentTests = samp.getTests();

                        // get this list of analyses BEFORE adding the pending tests to list (pending
                        // tests should not get a printed date)
                        // bugzilla 1900 do not update printedDate on preview
                        if (!resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)) {
                            for (int i = 0; i < currentTests.size(); i++) {
                                ResultsReportTest test = (ResultsReportTest) currentTests.get(i);
                                // don't overwrite printed date on previously reported (amended) tests
                                // but collect ones that are printed today (printed date is equal to today's
                                // date)
                                // so we can update the database with the date also
                                if (test.getPrintedDate() != null && test.getPrintedDate().equals(dateAsText)) {
                                    analysesPrinted.add(test.getAnalysis());
                                }
                            }
                        }
                        // get the pending tests as well for this sample:
                        List pendingAnalyses = null;
                        // bugzilla 1900
                        if (resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)) {
                            pendingAnalyses = analysisService
                                    .getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(samp.getSample());
                        } else {
                            pendingAnalyses = analysisService
                                    .getMaxRevisionPendingAnalysesReadyToBeReportedBySample(samp.getSample());
                        }

                        List pendingReportTests = populatePendingTests(pendingAnalyses);

                        // bugzilla 2027
                        List previouslyReportedTests = analysisService
                                .getAnalysesAlreadyReportedBySample(samp.getSample());
                        previouslyReportedTests = populateTests(previouslyReportedTests, PREVIOUS_SECTION);

                        // sort the pending tests
                        pendingReportTests = completeHierarchyOfTestsForSorting(pendingReportTests);
                        List totalList = sortTests(pendingReportTests);
                        totalList = removePhantomTests(totalList);

                        // sort the tests with results to report
                        // first bring over any possible parent test hierarchies from
                        // previouslyReportedTests
                        moveParentTestsOfCurrentTestsFromPreviouslyReportedTests(currentTests, previouslyReportedTests);
                        currentTests = completeHierarchyOfTestsForSorting(currentTests);
                        currentTests = sortTests(currentTests);
                        currentTests = removePhantomTests(currentTests);
                        // bugzilla 1900
                        if (resultsReportType.equals(RESULTS_REPORT_TYPE_AMENDED)
                                || resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)) {
                            currentTests = addPreviouslyReportedForAmendedTests(currentTests);
                        }
                        // bugzilla 1900
                        populateTestsWithResults(currentTests);
                        totalList.addAll(currentTests);

                        // sort the previously reported tests
                        previouslyReportedTests = completeHierarchyOfTestsForSorting(previouslyReportedTests);
                        previouslyReportedTests = sortTests(previouslyReportedTests);
                        previouslyReportedTests = removePhantomTests(previouslyReportedTests);
                        // bugzilla 1900
                        populateTestsWithResults(previouslyReportedTests);
                        totalList.addAll(previouslyReportedTests);

                        JRHibernateDataSource testsDS = new JRHibernateDataSource(totalList);
                        samp.setResultsReportTests(testsDS);

                        samplesList.add(samp);
                    }

                    // sort samples by organization id and accessionNumber
                    Collections.sort(samplesList, ResultsReportSampleComparator.VALUE_COMPARATOR);

                    JRHibernateDataSource ds = new JRHibernateDataSource(samplesList);

                    parameters.put("Report_Datasource", ds);

                    // Finally, we have to be able to call the Hibernate data source from
                    // JasperReports. To do so, we start by looking at the JasperManager
                    // fillReport()
                    // method, which takes a JRDataSource object as its third parameter and uses it
                    // to generate the report :

                    // bugzilla 1900 moved this to here
                    byte[] bytes = null;
                    bytes = JasperRunManager.runReportToPdf(mainReportFile.getPath(), parameters, ds);

                    // bugzilla 1900 moved this to here to fix java.lang.IllegalStateException:
                    // getOutputStream() has already been called
                    ServletOutputStream servletOutputStream = response.getOutputStream();
                    response.setContentType("application/pdf");
                    response.setContentLength(bytes.length);

                    servletOutputStream.write(bytes, 0, bytes.length);
                    servletOutputStream.flush();
                    servletOutputStream.close();

                    UserSessionData usd = (UserSessionData) request.getSession()
                            .getAttribute(IActionConstants.USER_SESSION_DATA);
                    String sysUserId = String.valueOf(usd.getSystemUserId());
                    for (int i = 0; i < analysesPrinted.size(); i++) {
                        Analysis analysis = (Analysis) analysesPrinted.get(i);
                        analysis.setSysUserId(sysUserId);
                        analysis.setPrintedDateForDisplay(dateAsText);
                        analysisService.update(analysis);
                    }
                    return true;

                } catch (JRException e) {
                    errors.reject("errors.jasperreport.general");
                    // bugzilla 2154
                    LogEvent.logError(e);
                    // rethrow an exception so transaction management detects it and rolls back
                    throw new LIMSRuntimeException(e);
                } catch (LIMSResultsReportHasNoDataException e) {
                    LogEvent.logError(e);
                    if (accessionNumbers.size() > 1) {
                        // message if report is for several samples
                        errors.reject("errors.jasperreport.resultsreports.nodata");
                    } else {
                        // message if report is for one sample
                        errors.reject("errors.jasperreport.resultsreport.nodata");
                    }
                    // rethrow an exception so transaction management detects it and rolls back
                    throw new LIMSRuntimeException(e);
                } catch (org.hibernate.StaleObjectStateException e) {
                    LogEvent.logError(e);
                    errors.reject("errors.OptimisticLockException");
                    // rethrow an exception so transaction management detects it and rolls back
                    throw new LIMSRuntimeException(e);
                } catch (IOException | RuntimeException e) {
                    LogEvent.logError(e);
                    errors.reject("errors.jasperreport.general");
                    // rethrow an exception so transaction management detects it and rolls back
                    throw new LIMSRuntimeException(e);
                } finally {
                    if (errors.hasErrors()) {
                        request.setAttribute(Constants.REQUEST_ERRORS, errors);
                    }
                }
            }
        });
    }

    // this is for current and previous tests
    private List populateTests(List listOfTests, int section) {
        // filter list of analyses to report (depending on whether amended/original
        // report
        // preload list of resultsReportTests - then process those in a later loop
        List testsToReport = new ArrayList();
        for (int i = 0; i < listOfTests.size(); i++) {
            String id = (String) listOfTests.get(i);
            Analysis analysis = new Analysis();
            analysis.setId(id);
            analysisService.getData(analysis);

            ResultsReportTest reportTest = new ResultsReportTest();
            reportTest.setAnalysis(analysis);
            String testName = TestServiceImpl.getUserLocalizedTestName(analysis.getTest());
            reportTest.setTestName(testName);
            // bugzilla 1900
            if (!resultsReportType.equals(RESULTS_REPORT_TYPE_PREVIEW)) {
                if (section == CURRENT_SECTION) {
                    reportTest.setPrintedDate(dateAsText);
                }
            }
            if (section == PREVIOUS_SECTION) {
                reportTest.setPrintedDate(analysis.getPrintedDateForDisplay());
            }
            String testMessage = "";
            // only do this for current test section
            if (section == CURRENT_SECTION) {
                if (!analysis.getRevision().equals("0")) {
                    testMessage = " " + amendedMessage;
                }
            }
            reportTest.setTestMessage(testMessage);
            reportTest.setTestDescription(TestServiceImpl.getLocalizedTestNameWithType(analysis.getTest()));
            reportTest.setTestId(analysis.getTest().getId());
            reportTest.setAnalysisId(analysis.getId());

            testsToReport.add(reportTest);
        }
        return testsToReport;
    }

    private List populatePendingTests(List pendingAnalyses) {
        List pendingReportTests = new ArrayList();
        for (int i = 0; i < pendingAnalyses.size(); i++) {
            String id = (String) pendingAnalyses.get(i);
            Analysis pendingAnalysis = new Analysis();
            pendingAnalysis.setId(id);
            analysisService.getData(pendingAnalysis);
            ResultsReportTest reportTest = new ResultsReportTest();
            reportTest.setAnalyteResults(null);
            reportTest.setAnalysis(pendingAnalysis);
            // this will be Testing Pending for other tests
            reportTest.setAnalysisStatus(testingPendingMessage);

            String testName = TestServiceImpl.getUserLocalizedTestName(pendingAnalysis.getTest());
            reportTest.setTestName(testName);
            reportTest.setTestMessage("");
            reportTest.setTestId(pendingAnalysis.getTest().getId());
            reportTest.setAnalysisId(pendingAnalysis.getId());
            reportTest.setTestDescription(TestServiceImpl.getLocalizedTestNameWithType(pendingAnalysis.getTest()));
            pendingReportTests.add(reportTest);
        }
        return pendingReportTests;
    }

    // bugzilla 1856
    protected List addPreviouslyReportedForAmendedTests(List currentTests) {
        if (currentTests != null && currentTests.size() > 0) {
            List tempCurrentTests = new ArrayList();
            for (int i = 0; i < currentTests.size(); i++) {
                ResultsReportTest test = (ResultsReportTest) currentTests.get(i);
                tempCurrentTests.add(test);
            }
            Iterator currentIt = tempCurrentTests.iterator();

            currentTests = new ArrayList();
            while (currentIt.hasNext()) {
                ResultsReportTest currentTest = (ResultsReportTest) currentIt.next();
                currentTests.add(currentTest);
                // only add a previous test IF this is a current test (not yet printed) AND
                // revision > 0
                if (!currentTest.getAnalysis().getRevision().equals("0")
                        && currentTest.getAnalysis().getPrintedDate() == null) {
                    // get original test also
                    Analysis previousAnalysis = analysisService
                            .getPreviousAnalysisForAmendedAnalysis(currentTest.getAnalysis());
                    ResultsReportTest reportTest = new ResultsReportTest();
                    reportTest.setAnalysis(previousAnalysis);
                    String testName = TestServiceImpl.getUserLocalizedTestName(previousAnalysis.getTest());
                    reportTest.setTestName(testName);
                    reportTest.setPrintedDate(previousAnalysis.getPrintedDateForDisplay());
                    String testMessage = " " + originalMessage;
                    reportTest.setTestMessage(testMessage);
                    reportTest.setTestDescription(TestServiceImpl.getUserLocalizedTestName(previousAnalysis.getTest()));
                    reportTest.setTestId(previousAnalysis.getTest().getId());
                    reportTest.setAnalysisId(previousAnalysis.getId());
                    currentTests.add(reportTest);
                }
            }
        }
        return currentTests;
    }

    private void populateTestsWithResults(List testsToReport) {
        for (int i = 0; i < testsToReport.size(); i++) {
            ResultsReportTest reportTest = (ResultsReportTest) testsToReport.get(i);
            String id = reportTest.getAnalysisId();
            Analysis analysis = new Analysis();
            analysis.setId(id);
            analysisService.getData(analysis);

            // get reportable results for test, and corresponding analyte information
            List results = resultService.getReportableResultsByAnalysis(analysis);

            // double-check that there is at least one reportable result
            if (results == null || results.size() == 0) {
                continue;
            }

            List analyteResults = new ArrayList();
            for (int j = 0; j < results.size(); j++) {
                Result result = (Result) results.get(j);
                reportAnalyteResult = new ResultsReportAnalyteResult();
                reportAnalyteResult.setResult(result);
                Analyte analyte = result.getAnalyte();
                reportAnalyteResult.setAnalyte(analyte);
                reportAnalyteResult.setComponentName(analyte.getAnalyteName());
                String resultValue = "";
                if (result.getResultType()
                        .equals(ConfigurationProperties.getInstance().getPropertyValue("dictionaryType"))) {
                    Dictionary dictionary = new Dictionary();
                    dictionary.setId(result.getValue());
                    dictionaryService.getData(dictionary);
                    resultValue = dictionary.getDictEntry();
                } else {
                    resultValue = result.getValue();
                }
                reportAnalyteResult.setResultValue(resultValue);
                String testResultSortOrder = "";
                testResultSortOrder = result.getSortOrder();
                reportAnalyteResult.setTestResultSortOrder(testResultSortOrder);
                analyteResults.add(reportAnalyteResult);
            }

            // sort the results by sort order of test result
            Collections.sort(analyteResults, ResultsReportAnalyteResultComparator.VALUE_COMPARATOR);
            reportTest.setAnalyteResults(analyteResults);
            reportTest.setAnalysisStatus(null);

            JRHibernateDataSource analyteResultsDS = new JRHibernateDataSource(analyteResults);
            reportTest.setResultsReportAnalyteResults(analyteResultsDS);
        }
    }
}
