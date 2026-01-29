package org.openelisglobal.result.utils;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.inventory.valueholder.InventoryEnums;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.label.valueholder.Label;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.reportconfiguration.valueholder.Report;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testtrailer.valueholder.TestTrailer;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

public class DemoData {

    private List<Localization> localizations;
    private List<SystemUser> systemUsers;
    private List<UnitOfMeasure> unitOfMeasures;
    private List<Report> reports;
    private List<TestTrailer> testTrailers;
    private List<Scriptlet> scriptlets;
    private List<Label> labels;
    private List<Method> methods;
    private List<StatusOfSample> statusOfSamples;
    private List<TypeOfSample> typeOfSamples;
    private List<Sample> samples;
    private List<SampleItem> sampleItems;
    private List<Organization> organizations;
    private List<TestSection> testSections;
    private List<Test> tests;
    private List<TestResult> testResults;
    private List<Analyte> analytes;
    private List<TestAnalyte> testAnalytes;
    private List<Panel> panels;
    private List<Analysis> analyses;
    private List<Result> results;
    private List<ResultSignature> resultSignatures;
    private List<TestReflex> testReflexes;
    private List<ReflexRule> reflexRules;
    private List<ReflexRuleCondition> reflexRuleConditions;
    private List<InventoryStorageLocation> inventoryStorageLocations;
    private List<InventoryItem> inventoryItems;
    private List<ResultInventory> resultInventories;
    private List<PatientIdentityType> patientIdentityTypes;
    private List<Person> persons;
    private List<Patient> patients;
    private Timestamp lastUpdated = new Timestamp(System.currentTimeMillis());

    // Initialize all data in constructor
    public DemoData() {
        initializeAllData();
    }

    private void initializeAllData() {
        // Initialize in dependency order
        localizations = createLocalizations();
        systemUsers = createSystemUsers();
        unitOfMeasures = createUnitOfMeasures();
        reports = createReports();
        testTrailers = createTestTrailers();
        scriptlets = createScriptlets();
        labels = createLabels();
        methods = createMethods();
        statusOfSamples = createStatusOfSamples();
        typeOfSamples = createTypeOfSamples();
        samples = createSamples();
        sampleItems = createSampleItems();
        organizations = createOrganizations();
        testSections = createTestSections();
        tests = createTests();
        testResults = createTestResults();
        analytes = createAnalytes();
        testAnalytes = createTestAnalytes();
        panels = createPanels();
        analyses = createAnalyses();
        results = createResults();
        resultSignatures = createResultSignatures();
        testReflexes = createTestReflexes();
        reflexRules = createReflexRules();
        reflexRuleConditions = createReflexRuleConditions();
        inventoryStorageLocations = createInventoryStorageLocations();
        inventoryItems = createInventoryItems();
        resultInventories = createResultInventories();
        patientIdentityTypes = createPatientIdentityTypes();
        persons = createPersons();
        patients = createPatients();
    }

    private List<Localization> createLocalizations() {
        List<Localization> localizationsList = new ArrayList<>();

        Localization loc1 = new Localization();
        loc1.setId("1");
        loc1.setDescription("Test Display Name");
        loc1.setEnglish("Complete Blood Count");
        loc1.setFrench("Numération formule sanguine complète");
        loc1.setLastupdated(lastUpdated);
        localizationsList.add(loc1);

        Localization loc2 = new Localization();
        loc2.setId("2");
        loc2.setDescription("Urinalysis Display");
        loc2.setEnglish("Urinalysis");
        loc2.setFrench("Analyse d'urine");
        loc2.setLastupdated(lastUpdated);
        localizationsList.add(loc2);

        Localization loc3 = new Localization();
        loc3.setId("3");
        loc3.setDescription("Chemistry Panel");
        loc3.setEnglish("Chemistry Panel");
        loc3.setFrench("Panneau de chimie");
        loc3.setLastupdated(lastUpdated);
        localizationsList.add(loc3);

        Localization loc4 = new Localization();
        loc4.setId("4");
        loc4.setDescription("Blood Sample");
        loc4.setEnglish("Blood");
        loc4.setFrench("Sang");
        loc4.setLastupdated(lastUpdated);
        localizationsList.add(loc4);

        Localization loc5 = new Localization();
        loc5.setId("5");
        loc5.setDescription("Urine Sample");
        loc5.setEnglish("Urine");
        loc5.setFrench("Urine");
        loc5.setLastupdated(lastUpdated);
        localizationsList.add(loc5);

        return localizationsList;
    }

    private List<SystemUser> createSystemUsers() {
        List<SystemUser> systemUsersList = new ArrayList<>();

        SystemUser user1 = new SystemUser();
        user1.setId("1");
        user1.setExternalId("EMP001");
        user1.setLoginName("labadmin");
        user1.setLastName("Johnson");
        user1.setFirstName("Robert");
        user1.setInitials("RJ");
        user1.setIsActive("Y");
        user1.setIsEmployee("Y");
        user1.setLastupdated(lastUpdated);
        systemUsersList.add(user1);

        SystemUser user2 = new SystemUser();
        user2.setId("2");
        user2.setExternalId("EMP002");
        user2.setLoginName("labtech1");
        user2.setLastName("Smith");
        user2.setFirstName("Jennifer");
        user2.setInitials("JS");
        user2.setIsActive("Y");
        user2.setIsEmployee("Y");
        user2.setLastupdated(lastUpdated);
        systemUsersList.add(user2);

        SystemUser user3 = new SystemUser();
        user3.setId("3");
        user3.setExternalId("EMP003");
        user3.setLoginName("labtech2");
        user3.setLastName("Williams");
        user3.setFirstName("Michael");
        user3.setInitials("MW");
        user3.setIsActive("Y");
        user3.setIsEmployee("Y");
        user3.setLastupdated(lastUpdated);
        systemUsersList.add(user3);

        return systemUsersList;
    }

    private List<UnitOfMeasure> createUnitOfMeasures() {
        List<UnitOfMeasure> unitOfMeasuresList = new ArrayList<>();

        UnitOfMeasure uom1 = new UnitOfMeasure();
        uom1.setId("1");
        uom1.setName("g/dL");
        uom1.setDescription("Grams per deciliter");
        uom1.setLastupdated(lastUpdated);
        unitOfMeasuresList.add(uom1);

        UnitOfMeasure uom2 = new UnitOfMeasure();
        uom2.setId("2");
        uom2.setName("mg/dL");
        uom2.setDescription("Milligrams per deciliter");
        uom2.setLastupdated(lastUpdated);
        unitOfMeasuresList.add(uom2);

        UnitOfMeasure uom3 = new UnitOfMeasure();
        uom3.setId("3");
        uom3.setName("μmol/L");
        uom3.setDescription("Micromoles per liter");
        uom3.setLastupdated(lastUpdated);
        unitOfMeasuresList.add(uom3);

        UnitOfMeasure uom4 = new UnitOfMeasure();
        uom4.setId("4");
        uom4.setName("U/L");
        uom4.setDescription("Units per liter");
        uom4.setLastupdated(lastUpdated);
        unitOfMeasuresList.add(uom4);

        UnitOfMeasure uom5 = new UnitOfMeasure();
        uom5.setId("5");
        uom5.setName("10^9/L");
        uom5.setDescription("Billion cells per liter");
        uom5.setLastupdated(lastUpdated);
        unitOfMeasuresList.add(uom5);

        return unitOfMeasuresList;
    }

    private List<Report> createReports() {
        List<Report> reportsList = new ArrayList<>();

        Report report1 = new Report();
        report1.setId("1");
        report1.setCategory("Patient Reports");
        report1.setSortOrder(1);
        report1.setMenuElementId("101");
        report1.setDisplayKey("menu.report.patient.results");
        report1.setName("Patient Results Report");
        report1.setIsVisible(true);
        report1.setLastupdated(lastUpdated);
        reportsList.add(report1);

        Report report2 = new Report();
        report2.setId("2");
        report2.setCategory("Management Reports");
        report2.setSortOrder(2);
        report2.setMenuElementId("102");
        report2.setDisplayKey("menu.report.workload");
        report2.setName("Workload Summary");
        report2.setIsVisible(true);
        report2.setLastupdated(lastUpdated);
        reportsList.add(report2);

        Report report3 = new Report();
        report3.setId("3");
        report3.setCategory("Quality Control");
        report3.setSortOrder(3);
        report3.setMenuElementId("103");
        report3.setDisplayKey("menu.report.qc");
        report3.setName("Quality Control Report");
        report3.setIsVisible(true);
        report3.setLastupdated(lastUpdated);
        reportsList.add(report3);

        return reportsList;
    }

    private List<TestTrailer> createTestTrailers() {
        List<TestTrailer> testTrailersList = new ArrayList<>();

        TestTrailer trailer1 = new TestTrailer();
        trailer1.setId("1");
        trailer1.setTestTrailerName("Normal Ranges");
        trailer1.setDescription("Normal reference ranges for tests");
        trailer1.setText("Normal Ranges:\nHemoglobin: 13.5-17.5 g/dL\nWBC: 4.5-11.0 x10^9/L\nGlucose: 70-100 mg/dL");
        trailer1.setLastupdated(lastUpdated);
        testTrailersList.add(trailer1);

        TestTrailer trailer2 = new TestTrailer();
        trailer2.setId("2");
        trailer2.setTestTrailerName("Collection Instructions");
        trailer2.setDescription("Sample collection instructions");
        trailer2.setText("Collect in appropriate container. Avoid hemolysis. Transport at room temperature.");
        trailer2.setLastupdated(lastUpdated);
        testTrailersList.add(trailer2);

        TestTrailer trailer3 = new TestTrailer();
        trailer3.setId("3");
        trailer3.setTestTrailerName("Interpretation");
        trailer3.setDescription("Test result interpretation");
        trailer3.setText("Results should be interpreted in clinical context. Consult physician for abnormal results.");
        trailer3.setLastupdated(lastUpdated);
        testTrailersList.add(trailer3);

        return testTrailersList;
    }

    private List<Scriptlet> createScriptlets() {
        List<Scriptlet> scriptletsList = new ArrayList<>();

        Scriptlet scriptlet1 = new Scriptlet();
        scriptlet1.setId("1");
        scriptlet1.setScriptletName("Calculate eGFR");
        scriptlet1.setCodeType("T");
        scriptlet1.setCodeSource(
                "function calculateEGFR(creatinine, age, gender) {\n  // CKD-EPI formula\n  var k = gender === 'M' ? 0.9 : 0.7;\n  var a = gender === 'M' ? -0.411 : -0.329;\n  return 141 * Math.pow(Math.min(creatinine/k, 1), a) * Math.pow(Math.max(creatinine/k, 1), -1.209) * Math.pow(0.993, age);\n}");
        scriptlet1.setLastupdated(lastUpdated);
        scriptletsList.add(scriptlet1);

        Scriptlet scriptlet2 = new Scriptlet();
        scriptlet2.setId("2");
        scriptlet2.setScriptletName("Check Critical Values");
        scriptlet2.setCodeType("T");
        scriptlet2.setCodeSource(
                "function checkCritical(value, testName) {\n  var criticalRanges = {\n    'Potassium': {low: 3.0, high: 6.0},\n    'Sodium': {low: 125, high: 155},\n    'Glucose': {low: 50, high: 400}\n  };\n  var range = criticalRanges[testName];\n  return range && (value < range.low || value > range.high);\n}");
        scriptlet2.setLastupdated(lastUpdated);
        scriptletsList.add(scriptlet2);

        Scriptlet scriptlet3 = new Scriptlet();
        scriptlet3.setId("3");
        scriptlet3.setScriptletName("Calculate LDL");
        scriptlet3.setCodeType("T");
        scriptlet3.setCodeSource(
                "function calculateLDL(cholesterol, hdl, triglycerides) {\n  // Friedewald formula\n  return cholesterol - hdl - (triglycerides/5);\n}");
        scriptlet3.setLastupdated(lastUpdated);
        scriptletsList.add(scriptlet3);

        return scriptletsList;
    }

    private List<Label> createLabels() {
        List<Label> labelsList = new ArrayList<>();

        Label label1 = new Label();
        label1.setId("1");
        label1.setName("Specimen Label");
        label1.setDescription("Label for specimen tubes");
        label1.setPrinterType("Z");
        label1.setScriptlet(scriptlets.get(0));
        label1.setLastupdated(lastUpdated);
        labelsList.add(label1);

        Label label2 = new Label();
        label2.setId("2");
        label2.setName("Patient Report Label");
        label2.setDescription("Label for patient reports");
        label2.setPrinterType("L");
        label2.setScriptlet(scriptlets.get(1));
        label2.setLastupdated(lastUpdated);
        labelsList.add(label2);

        Label label3 = new Label();
        label3.setId("3");
        label3.setName("Slide Label");
        label3.setDescription("Label for microscope slides");
        label3.setPrinterType("S");
        label3.setScriptlet(scriptlets.get(2));
        label3.setLastupdated(lastUpdated);
        labelsList.add(label3);

        return labelsList;
    }

    private List<Method> createMethods() {
        List<Method> methodsList = new ArrayList<>();

        Method method1 = new Method();
        method1.setId("1");
        method1.setName("Automated Hematology Analyzer");
        method1.setDescription("Automated cell counting using impedance technology");
        method1.setLocalization(localizations.get(0));
        method1.setReportingDescription("CBC by automated analyzer");
        Date date = Date.valueOf("2015-01-01");
        method1.setActiveBeginDate(date);
        method1.setLastupdated(lastUpdated);
        methodsList.add(method1);

        Method method2 = new Method();
        method2.setId("2");
        method2.setName("Dipstick Urinalysis");
        method2.setDescription("Chemical analysis using reagent strips");
        method2.setLocalization(localizations.get(1));
        method2.setReportingDescription("Urinalysis by dipstick");
        method2.setActiveBeginDate(Date.valueOf("2016-03-15"));
        method2.setActiveEndDate(null);
        method2.setLastupdated(lastUpdated);
        methodsList.add(method2);

        Method method3 = new Method();
        method3.setId("3");
        method3.setName("Spectrophotometry");
        method3.setDescription("Chemical analysis using spectrophotometer");
        method3.setLocalization(localizations.get(2));
        method3.setReportingDescription("Chemistry by spectrophotometry");
        method3.setActiveBeginDate(Date.valueOf("2017-06-01"));
        method3.setActiveEndDate(null);
        method3.setLastupdated(lastUpdated);
        methodsList.add(method3);

        return methodsList;
    }

    private List<StatusOfSample> createStatusOfSamples() {
        List<StatusOfSample> statusOfSamplesList = new ArrayList<>();

        StatusOfSample status1 = new StatusOfSample();
        status1.setId("1");
        status1.setDescription("Sample Entered");
        status1.setCode("1");
        status1.setStatusType("Sample");
        status1.setLastupdated(lastUpdated);
        status1.setName("Entered");
        status1.setKey("status.entered");
        status1.setIsActive("Y");
        statusOfSamplesList.add(status1);

        StatusOfSample status2 = new StatusOfSample();
        status2.setId("2");
        status2.setDescription("Sample In Progress");
        status2.setCode("2");
        status2.setStatusType("Sample");
        status2.setLastupdated(lastUpdated);
        status2.setName("In Progress");
        status2.setKey("status.inprogress");
        status2.setIsActive("Y");
        statusOfSamplesList.add(status2);

        StatusOfSample status3 = new StatusOfSample();
        status3.setId("3");
        status3.setDescription("Sample Completed");
        status3.setCode("3");
        status3.setStatusType("Sample");
        status3.setLastupdated(lastUpdated);
        status3.setName("Completed");
        status3.setKey("status.completed");
        status3.setIsActive("Y");
        statusOfSamplesList.add(status3);

        StatusOfSample status4 = new StatusOfSample();
        status4.setId("4");
        status4.setDescription("Sample Rejected");
        status4.setCode("4");
        status4.setStatusType("Sample");
        status4.setLastupdated(lastUpdated);
        status4.setName("Rejected");
        status4.setKey("status.rejected");
        status4.setIsActive("Y");
        statusOfSamplesList.add(status4);

        return statusOfSamplesList;
    }

    private List<TypeOfSample> createTypeOfSamples() {
        List<TypeOfSample> typeOfSamplesList = new ArrayList<>();

        TypeOfSample type1 = new TypeOfSample();
        type1.setId("1");
        type1.setDescription("Whole Blood");
        type1.setDomain("H");
        type1.setLocalization(localizations.get(3));
        type1.setLastupdated(lastUpdated);
        type1.setIsActive(true);
        type1.setSortOrder(1);
        typeOfSamplesList.add(type1);

        TypeOfSample type2 = new TypeOfSample();
        type2.setId("2");
        type2.setDescription("Urine");
        type2.setDomain("H");
        type2.setLocalization(localizations.get(4));
        type2.setLastupdated(lastUpdated);
        type2.setIsActive(true);
        type2.setSortOrder(2);
        typeOfSamplesList.add(type2);

        TypeOfSample type3 = new TypeOfSample();
        type3.setId("3");
        type3.setDescription("Serum");
        type3.setDomain("H");
        type3.setLocalization(localizations.get(3));
        type3.setLastupdated(lastUpdated);
        type3.setIsActive(true);
        type3.setSortOrder(3);
        typeOfSamplesList.add(type3);

        return typeOfSamplesList;
    }

    private List<Sample> createSamples() {
        List<Sample> samplesList = new ArrayList<>();

        Sample sample1 = new Sample();
        sample1.setId("1");
        sample1.setAccessionNumber("24-000001");
        sample1.setStatusId("3"); // Completed
        sample1.setReceivedDate(Date.valueOf("2024-01-15"));
        sample1.setEnteredDate(Date.valueOf("2024-01-15"));
        Timestamp collectDate = Timestamp.valueOf("2024-01-15 08:30:00");
        sample1.setCollectionDate(collectDate);
        sample1.setLastupdated(lastUpdated);
        sample1.setDomain("H");
        sample1.setSysUserId("1");
        samplesList.add(sample1);

        Sample sample2 = new Sample();
        sample2.setId("2");
        sample2.setAccessionNumber("24-000002");
        sample2.setStatusId("2");
        sample2.setReceivedDate(Date.valueOf("2024-01-16"));
        sample2.setEnteredDate(Date.valueOf("2024-01-16"));
        sample2.setCollectionDate(Timestamp.valueOf("2024-01-16 09:15:00"));
        sample2.setLastupdated(lastUpdated);
        sample2.setDomain("H");
        sample2.setSysUserId("1");
        samplesList.add(sample2);

        Sample sample3 = new Sample();
        sample3.setId("3");
        sample3.setAccessionNumber("24-000003");
        sample3.setStatusId("1");
        sample3.setReceivedDate(Date.valueOf("2024-01-17"));
        sample3.setEnteredDate(Date.valueOf("2024-01-17"));
        sample3.setCollectionDate(Timestamp.valueOf("2024-01-17 10:45:00"));
        sample3.setLastupdated(lastUpdated);
        sample3.setDomain("H");
        sample3.setSysUserId("2");
        samplesList.add(sample3);

        return samplesList;
    }

    private List<SampleItem> createSampleItems() {
        List<SampleItem> sampleItemsList = new ArrayList<>();

        SampleItem item1 = new SampleItem();
        item1.setId("1");
        item1.setSortOrder("1");
        item1.setStatusId("3");
        item1.setSample(samples.get(0));
        item1.setTypeOfSample(typeOfSamples.get(0)); // Whole Blood
        Timestamp collectionDate = Timestamp.valueOf("2024-01-15 08:30:00");
        item1.setCollectionDate(collectionDate);
        item1.setLastupdated(lastUpdated);
        item1.setSourceOther("Left arm");
        sampleItemsList.add(item1);

        SampleItem item2 = new SampleItem();
        item2.setId("2");
        item2.setSortOrder("1");
        item2.setStatusId("2");
        item2.setSample(samples.get(1));
        item2.setTypeOfSample(typeOfSamples.get(1)); // Urine
        item2.setCollectionDate(Timestamp.valueOf("2024-01-16 09:15:00"));
        item2.setLastupdated(lastUpdated);
        item2.setSourceOther("Midstream");
        sampleItemsList.add(item2);

        SampleItem item3 = new SampleItem();
        item3.setId("3");
        item3.setSortOrder("1");
        item3.setStatusId("1");
        item3.setSample(samples.get(2));
        item3.setTypeOfSample(typeOfSamples.get(2)); // Serum
        item3.setCollectionDate(Timestamp.valueOf("2024-01-17 10:45:00"));
        item3.setLastupdated(lastUpdated);
        item3.setSourceOther("Right arm");
        sampleItemsList.add(item3);

        return sampleItemsList;
    }

    private List<Organization> createOrganizations() {
        List<Organization> organizationsList = new ArrayList<>();

        Organization org1 = new Organization();
        org1.setId("1");
        org1.setLastupdated(lastUpdated);
        org1.setName("Main Hospital Laboratory");
        org1.setCity("Metropolis");
        org1.setZipCode("12345");
        org1.setShortName("MAINLAB");
        org1.setMultipleUnit("Central Laboratory");
        org1.setStreetAddress("123 Medical Center Drive");
        org1.setState("CA");
        org1.setInternetAddress("http://www.mainhospital.org/lab");
        org1.setCliaNum("12D1234567");
        org1.setPwsId("PWS123456");
        org1.setOrganizationLocalAbbreviation("MHL");
        org1.setCode("ORG001");
        organizationsList.add(org1);

        Organization org2 = new Organization();
        org2.setId("2");
        org2.setLastupdated(lastUpdated);
        org2.setName("Reference Laboratory");
        org2.setCity("Centerville");
        org2.setZipCode("67890");
        org2.setShortName("REFLAB");
        org2.setMultipleUnit("Reference Testing Division");
        org2.setStreetAddress("456 Reference Way");
        org2.setState("NY");
        org2.setInternetAddress("http://www.reflab.com");
        org2.setCliaNum("34D7654321");
        org2.setPwsId("PWS654321");
        org2.setOrganizationLocalAbbreviation("RL");
        org2.setCode("ORG002");
        organizationsList.add(org2);

        Organization org3 = new Organization();
        org3.setId("3");
        org3.setLastupdated(lastUpdated);
        org3.setName("Clinics Network Laboratory");
        org3.setCity("Springfield");
        org3.setZipCode("54321");
        org3.setShortName("CLINICLAB");
        org3.setMultipleUnit("Outpatient Network");
        org3.setStreetAddress("789 Clinic Boulevard");
        org3.setState("IL");
        org3.setInternetAddress("http://www.clinicnetwork.org");
        org3.setCliaNum("56D9876543");
        org3.setPwsId("PWS987654");
        org3.setOrganizationLocalAbbreviation("CNL");
        org3.setCode("ORG003");
        organizationsList.add(org3);

        return organizationsList;
    }

    private List<TestSection> createTestSections() {
        List<TestSection> testSectionsList = new ArrayList<>();

        TestSection section1 = new TestSection();
        section1.setId("1");
        section1.setName("Hematology");
        section1.setDescription("Blood cell analysis section");
        section1.setOrganization(organizations.get(0));
        section1.setIsExternal("N");
        section1.setLastupdated(lastUpdated);
        section1.setSortOrder("1");
        section1.setLocalization(localizations.get(0));
        section1.setKey("testsection.hematology");
        section1.setIsActive("Y");
        testSectionsList.add(section1);

        TestSection section2 = new TestSection();
        section2.setId("2");
        section2.setName("Urinalysis");
        section2.setDescription("Urine analysis section");
        section2.setOrganization(organizations.get(0));
        section2.setIsExternal("N");
        section2.setLastupdated(lastUpdated);
        section2.setSortOrder("2");
        section2.setLocalization(localizations.get(1));
        section2.setKey("testsection.urinalysis");
        section2.setIsActive("Y");
        testSectionsList.add(section2);

        TestSection section3 = new TestSection();
        section3.setId("3");
        section3.setName("Chemistry");
        section3.setDescription("Clinical chemistry section");
        section3.setOrganization(organizations.get(0));
        section3.setIsExternal("N");
        section3.setLastupdated(lastUpdated);
        section3.setSortOrder("3");
        section3.setLocalization(localizations.get(2));
        section3.setKey("testsection.chemistry");
        section3.setIsActive("Y");
        testSectionsList.add(section3);

        return testSectionsList;
    }

    private List<Test> createTests() {
        List<Test> testsList = new ArrayList<>();

        // CBC Test
        Test test1 = new Test();
        test1.setId("1");
        test1.setMethod(methods.get(0));
        test1.setUnitOfMeasure(unitOfMeasures.get(4)); // 10^9/L
        test1.setDescription("Complete Blood Count");
        test1.setLoinc("58410-2");
        test1.setDescription("Complete blood count with differential");
        test1.setActiveBeginDate(Date.valueOf("2015-01-01"));
        test1.setActiveEndDate(null);
        test1.setTimeHolding("48");
        test1.setTimeWait("2");
        test1.setTimeAverage("30");
        test1.setTimeWarning("60");
        test1.setTimeMax("120");
        test1.setLabelQuantity("1");
        test1.setLastupdated(lastUpdated);
        test1.setLabel(labels.get(0));
        test1.setTestTrailer(testTrailers.get(0));
        test1.setTestSection(testSections.get(0)); // Hematology
        test1.setScriptlet(scriptlets.get(0));
        test1.setLocalCode("CBC");
        test1.setSortOrder("10");
        test1.setName("Complete Blood Count");
        test1.setOrderable(true);
        test1.setGuid(UUID.randomUUID().toString());
        test1.setAntimicrobialResistance(false);
        test1.setIsReportable("Y");
        testsList.add(test1);

        // Urinalysis Test
        Test test2 = new Test();
        test2.setId("2");
        test2.setMethod(methods.get(1));
        test2.setUnitOfMeasure(null); // Qualitative test
        test2.setDescription("Urinalysis, Complete");
        test2.setLoinc("5811-5");
        test2.setDescription("Complete urinalysis with microscopy");
        test2.setActiveBeginDate(Date.valueOf("2016-03-15"));
        test2.setActiveEndDate(null);
        test2.setTimeHolding("24");
        test2.setTimeWait("1");
        test2.setTimeAverage("20");
        test2.setTimeWarning("40");
        test2.setTimeMax("60");
        test2.setLabelQuantity("1");
        test2.setLastupdated(lastUpdated);
        test2.setLabel(labels.get(1));
        test2.setTestTrailer(testTrailers.get(1));
        test2.setTestSection(testSections.get(1)); // Urinalysis
        test2.setScriptlet(scriptlets.get(1));
        test2.setLocalCode("UA");
        test2.setSortOrder("20");
        test2.setName("Urinalysis");
        test2.setOrderable(true);
        test2.setGuid(UUID.randomUUID().toString());
        test2.setAntimicrobialResistance(false);
        test2.setIsReportable("Y");
        testsList.add(test2);

        // Glucose Test
        Test test3 = new Test();
        test3.setId("3");
        test3.setMethod(methods.get(2));
        test3.setUnitOfMeasure(unitOfMeasures.get(1));
        test3.setDescription("Glucose, Serum");
        test3.setLoinc("2345-7");
        test3.setDescription("Glucose level in serum");
        test3.setActiveBeginDate(Date.valueOf("2017-06-01"));
        test3.setActiveEndDate(null);
        test3.setTimeHolding("24");
        test3.setTimeWait("1");
        test3.setTimeAverage("15");
        test3.setTimeWarning("30");
        test3.setTimeMax("45");
        test3.setLabelQuantity("1");
        test3.setLastupdated(lastUpdated);
        test3.setLabel(labels.get(2));
        test3.setTestTrailer(testTrailers.get(2));
        test3.setTestSection(testSections.get(2)); // Chemistry
        test3.setScriptlet(scriptlets.get(2));
        test3.setLocalCode("GLUC");
        test3.setSortOrder("30");
        test3.setName("Glucose");
        test3.setOrderable(true);
        test3.setGuid(UUID.randomUUID().toString());
        test3.setAntimicrobialResistance(false);
        test3.setIsReportable("Y");
        testsList.add(test3);

        return testsList;
    }

    private List<TestResult> createTestResults() {
        List<TestResult> testResultsList = new ArrayList<>();

        // CBC Results
        TestResult result1 = new TestResult();
        result1.setId("1");
        result1.setTest(tests.get(0));
        result1.setResultGroup("1");
        result1.setFlags("N");
        result1.setTestResultType("N");
        result1.setValue("4.5");
        result1.setSignificantDigits("1");
        result1.setQuantLimit("20.0");
        result1.setContLevel("Normal");
        result1.setLastupdated(lastUpdated);
        result1.setScriptlet(scriptlets.get(0));
        result1.setSortOrder("1");
        result1.setIsQuantifiable(true);
        result1.setIsActive(true);
        result1.setIsNormal(true);
        testResultsList.add(result1);

        TestResult result2 = new TestResult();
        result2.setId("2");
        result2.setTest(tests.get(0));
        result2.setResultGroup("2");
        result2.setFlags("H");
        result2.setTestResultType("N");
        result2.setValue("15.5");
        result2.setSignificantDigits("1");
        result2.setQuantLimit("20.0");
        result2.setContLevel("High");
        result2.setLastupdated(lastUpdated);
        result2.setScriptlet(scriptlets.get(0));
        result2.setSortOrder("2");
        result2.setIsQuantifiable(true);
        result2.setIsActive(true);
        result2.setIsNormal(false);
        testResultsList.add(result2);

        // Urinalysis Results
        TestResult result3 = new TestResult();
        result3.setId("3");
        result3.setTest(tests.get(1));
        result3.setResultGroup("1");
        result3.setFlags("N");
        result3.setTestResultType("D");
        result3.setValue("NEG");
        result3.setSignificantDigits("0");
        result3.setQuantLimit(null);
        result3.setContLevel("Normal");
        result3.setLastupdated(lastUpdated);
        result3.setScriptlet(scriptlets.get(1));
        result3.setSortOrder("1");
        result3.setIsQuantifiable(false);
        result3.setIsActive(true);
        result3.setIsNormal(true);
        testResultsList.add(result3);

        TestResult result4 = new TestResult();
        result4.setId("4");
        result4.setTest(tests.get(1));
        result4.setResultGroup("2");
        result4.setFlags("P");
        result4.setTestResultType("D");
        result4.setValue("1+");
        result4.setSignificantDigits("0");
        result4.setQuantLimit(null);
        result4.setContLevel("Abnormal");
        result4.setLastupdated(lastUpdated);
        result4.setScriptlet(scriptlets.get(1));
        result4.setSortOrder("2");
        result4.setIsQuantifiable(false);
        result4.setIsActive(true);
        result4.setIsNormal(false);
        testResultsList.add(result4);

        // Glucose Results
        TestResult result5 = new TestResult();
        result5.setId("5");
        result5.setTest(tests.get(2));
        result5.setResultGroup("1");
        result5.setFlags("N");
        result5.setTestResultType("N");
        result5.setValue("95");
        result5.setSignificantDigits("0");
        result5.setQuantLimit("500");
        result5.setContLevel("Normal");
        result5.setLastupdated(lastUpdated);
        result5.setScriptlet(scriptlets.get(2));
        result5.setSortOrder("1");
        result5.setIsQuantifiable(true);
        result5.setIsActive(true);
        result5.setIsNormal(true);
        testResultsList.add(result5);

        TestResult result6 = new TestResult();
        result6.setId("6");
        result6.setTest(tests.get(2));
        result6.setResultGroup("2");
        result6.setFlags("H");
        result6.setTestResultType("N");
        result6.setValue("210");
        result6.setSignificantDigits("0");
        result6.setQuantLimit("500");
        result6.setContLevel("High");
        result6.setLastupdated(lastUpdated);
        result6.setScriptlet(scriptlets.get(2));
        result6.setSortOrder("2");
        result6.setIsQuantifiable(true);
        result6.setIsActive(true);
        result6.setIsNormal(false);
        testResultsList.add(result6);

        return testResultsList;
    }

    private List<Analyte> createAnalytes() {
        List<Analyte> analytesList = new ArrayList<>();

        Analyte analyte1 = new Analyte();
        analyte1.setId("1");
        analyte1.setAnalyte(null);
        analyte1.setName("White Blood Cells");
        analyte1.setExternalId("AN001");
        analyte1.setLastupdated(lastUpdated);
        analyte1.setLocalAbbreviation("WBC");
        analytesList.add(analyte1);

        Analyte analyte2 = new Analyte();
        analyte2.setId("2");
        analyte2.setAnalyte(null);
        analyte2.setName("Hemoglobin");
        analyte2.setExternalId("AN002");
        analyte2.setLastupdated(lastUpdated);
        analyte2.setLocalAbbreviation("HGB");
        analytesList.add(analyte2);

        Analyte analyte3 = new Analyte();
        analyte3.setId("3");
        analyte3.setAnalyte(null);
        analyte3.setName("Glucose");
        analyte3.setExternalId("AN003");
        analyte3.setLastupdated(lastUpdated);
        analyte3.setLocalAbbreviation("GLU");
        analytesList.add(analyte3);

        Analyte analyte4 = new Analyte();
        analyte4.setId("4");
        analyte4.setAnalyte(null);
        analyte4.setName("Protein");
        analyte4.setExternalId("AN004");
        analyte4.setLastupdated(lastUpdated);
        analyte4.setLocalAbbreviation("PRO");
        analytesList.add(analyte4);

        return analytesList;
    }

    private List<TestAnalyte> createTestAnalytes() {
        List<TestAnalyte> testAnalytesList = new ArrayList<>();

        TestAnalyte testAnalyte1 = new TestAnalyte();
        testAnalyte1.setId("1");
        testAnalyte1.setTest(tests.get(0));
        testAnalyte1.setAnalyte(analytes.get(0)); // WBC
        testAnalyte1.setResultGroup("1");
        testAnalyte1.setSortOrder("10");
        testAnalyte1.setTestAnalyteType("R");
        testAnalyte1.setLastupdated(lastUpdated);
        testAnalyte1.setIsReportable("Y");
        testAnalytesList.add(testAnalyte1);

        TestAnalyte testAnalyte2 = new TestAnalyte();
        testAnalyte2.setId("2");
        testAnalyte2.setTest(tests.get(0));
        testAnalyte2.setAnalyte(analytes.get(1)); // Hemoglobin
        testAnalyte2.setResultGroup("2");
        testAnalyte2.setSortOrder("20");
        testAnalyte2.setTestAnalyteType("R");
        testAnalyte2.setLastupdated(lastUpdated);
        testAnalyte2.setIsReportable("Y");
        testAnalytesList.add(testAnalyte2);

        TestAnalyte testAnalyte3 = new TestAnalyte();
        testAnalyte3.setId("3");
        testAnalyte3.setTest(tests.get(1));
        testAnalyte3.setAnalyte(analytes.get(3)); // Protein
        testAnalyte3.setResultGroup("1");
        testAnalyte3.setSortOrder("10");
        testAnalyte3.setTestAnalyteType("R");
        testAnalyte3.setLastupdated(lastUpdated);
        testAnalyte3.setIsReportable("Y");
        testAnalytesList.add(testAnalyte3);

        TestAnalyte testAnalyte4 = new TestAnalyte();
        testAnalyte4.setId("4");
        testAnalyte4.setTest(tests.get(2));
        testAnalyte4.setAnalyte(analytes.get(2)); // Glucose
        testAnalyte4.setResultGroup("1");
        testAnalyte4.setSortOrder("10");
        testAnalyte4.setTestAnalyteType("R");
        testAnalyte4.setLastupdated(lastUpdated);
        testAnalyte4.setIsReportable("Y");
        testAnalytesList.add(testAnalyte4);

        return testAnalytesList;
    }

    private List<Panel> createPanels() {
        List<Panel> panelsList = new ArrayList<>();

        Panel panel1 = new Panel();
        panel1.setId("1");
        panel1.setName("Complete Blood Count");
        panel1.setDescription("Complete blood count panel including differential");
        panel1.setLocalization(localizations.get(0));
        panel1.setLoinc("58410-2");
        panel1.setLastupdated(lastUpdated);
        panel1.setIsActive("Y");
        panelsList.add(panel1);

        Panel panel2 = new Panel();
        panel2.setId("2");
        panel2.setName("Basic Metabolic Panel");
        panel2.setDescription("Basic metabolic panel tests");
        panel2.setLocalization(localizations.get(2));
        panel2.setLoinc("24323-8");
        panel2.setLastupdated(lastUpdated);
        panel2.setIsActive("Y");
        panelsList.add(panel2);

        Panel panel3 = new Panel();
        panel3.setId("3");
        panel3.setName("Urinalysis Panel");
        panel3.setDescription("Complete urinalysis panel");
        panel3.setLocalization(localizations.get(1));
        panel3.setLoinc("24356-8");
        panel3.setLastupdated(lastUpdated);
        panel3.setIsActive("Y");
        panelsList.add(panel3);

        return panelsList;
    }

    private List<Analysis> createAnalyses() {
        List<Analysis> analysesList = new ArrayList<>();

        // CBC Analysis
        Analysis analysis1 = new Analysis();
        analysis1.setId("1");
        analysis1.setSampleItem(sampleItems.get(0));
        analysis1.setTestSection(testSections.get(0));
        analysis1.setTest(tests.get(0)); // CBC
        analysis1.setRevision("1");
        analysis1.setStatus("Completed");
        analysis1.setStartedDate(Date.valueOf("2024-01-15"));
        analysis1.setCompletedDate(Date.valueOf("2024-01-15"));
        analysis1.setReleasedDate(Date.valueOf("2024-01-15"));
        analysis1.setPrintedDate(Date.valueOf("2024-01-16"));
        analysis1.setSoSendReadyDate(null);
        analysis1.setSoClientReference(null);
        analysis1.setSoNotifyReceivedDate(null);
        analysis1.setSoNotifySendDate(null);
        analysis1.setSoSendDate(null);
        analysis1.setSoSendEntryBy(null);
        analysis1.setSoSendEntryDate(null);
        analysis1.setAnalysisType("ROUTINE");
        analysis1.setLastupdated(lastUpdated);
        analysis1.setIsReportable("Y");
        analysis1.setTriggeredReflex(false);
        analysis1.setStatusId("3"); // Completed
        Timestamp enteredDate = Timestamp.valueOf("2024-01-15 09:00:00");
        analysis1.setEnteredDate(enteredDate);
        analysis1.setPanel(panels.get(0)); // CBC Panel
        analysis1.setReferredOut(false);
        analysis1.setSampleTypeName("Whole Blood");
        analysis1.setCorrectedSincePatientReport(false);
        analysis1.setFhirUuid(UUID.randomUUID());
        analysis1.setResultCalculated(false);
        analysis1.setMethod(methods.get(0));
        analysis1.setSysUserId("2");
        analysesList.add(analysis1);

        // Urinalysis Analysis
        Analysis analysis2 = new Analysis();
        analysis2.setId("2");
        analysis2.setSampleItem(sampleItems.get(1));
        analysis2.setTestSection(testSections.get(1)); // Urinalysis
        analysis2.setTest(tests.get(1)); // Urinalysis
        analysis2.setRevision("1");
        analysis2.setStatus("In Progress");
        analysis2.setStartedDate(Date.valueOf("2024-01-16"));
        analysis2.setCompletedDate(null);
        analysis2.setReleasedDate(null);
        analysis2.setPrintedDate(null);
        analysis2.setSoSendReadyDate(null);
        analysis2.setSoClientReference(null);
        analysis2.setSoNotifyReceivedDate(null);
        analysis2.setSoNotifySendDate(null);
        analysis2.setSoSendDate(null);
        analysis2.setSoSendEntryBy(null);
        analysis2.setSoSendEntryDate(null);
        analysis2.setAnalysisType("ROUTINE");
        analysis2.setLastupdated(lastUpdated);
        analysis2.setIsReportable("Y");
        analysis2.setTriggeredReflex(false);
        analysis2.setStatusId("2"); // In Progress
        analysis2.setEnteredDate(Timestamp.valueOf("2024-01-16 10:00:00"));
        analysis2.setPanel(panels.get(2)); // Urinalysis Panel
        analysis2.setReferredOut(false);
        analysis2.setSampleTypeName("Urine");
        analysis2.setCorrectedSincePatientReport(false);
        analysis2.setFhirUuid(UUID.randomUUID());
        analysis2.setResultCalculated(false);
        analysis2.setMethod(methods.get(1));
        analysis2.setSysUserId("3");
        analysesList.add(analysis2);

        // Glucose Analysis
        Analysis analysis3 = new Analysis();
        analysis3.setId("3");
        analysis3.setSampleItem(sampleItems.get(2));
        analysis3.setTestSection(testSections.get(2)); // Chemistry
        analysis3.setTest(tests.get(2)); // Glucose
        analysis3.setRevision("1");
        analysis3.setStatus("Entered");
        analysis3.setStartedDate(null);
        analysis3.setCompletedDate(null);
        analysis3.setReleasedDate(null);
        analysis3.setPrintedDate(null);
        analysis3.setSoSendReadyDate(null);
        analysis3.setSoClientReference(null);
        analysis3.setSoNotifyReceivedDate(null);
        analysis3.setSoNotifySendDate(null);
        analysis3.setSoSendDate(null);
        analysis3.setSoSendEntryBy(null);
        analysis3.setSoSendEntryDate(null);
        analysis3.setAnalysisType("STAT");
        analysis3.setLastupdated(lastUpdated);
        analysis3.setIsReportable("Y");
        analysis3.setTriggeredReflex(false);
        analysis3.setStatusId("1"); // Entered
        analysis3.setEnteredDate(Timestamp.valueOf("2024-01-17 11:00:00"));
        analysis3.setPanel(panels.get(1)); // Metabolic Panel
        analysis3.setReferredOut(false);
        analysis3.setSampleTypeName("Serum");
        analysis3.setCorrectedSincePatientReport(false);
        analysis3.setFhirUuid(UUID.randomUUID());
        analysis3.setResultCalculated(false);
        analysis3.setMethod(methods.get(2));
        analysis3.setSysUserId("2");
        analysesList.add(analysis3);

        return analysesList;
    }

    private List<Result> createResults() {
        List<Result> resultsList = new ArrayList<>();

        // WBC Result
        Result result1 = new Result();
        result1.setId("1");
        result1.setAnalysis(analyses.get(0)); // CBC Analysis
        result1.setSortOrder("10");
        result1.setResultType("N");
        result1.setValue("6.8");
        result1.setAnalyte(analytes.get(0)); // WBC
        result1.setTestResult(testResults.get(0));
        result1.setIsReportable("Y");
        result1.setLastupdated(lastUpdated);
        result1.setMinNormal(4.5);
        result1.setMaxNormal(11.0);
        result1.setSignificantDigits(1);
        result1.setGrouping(1);
        result1.setFhirUuid(UUID.randomUUID());
        result1.setSysUserId("2");
        resultsList.add(result1);

        // Hemoglobin Result
        Result result2 = new Result();
        result2.setId("2");
        result2.setAnalysis(analyses.get(0)); // CBC Analysis
        result2.setSortOrder("20");
        result2.setResultType("N");
        result2.setValue("14.2");
        result2.setAnalyte(analytes.get(1)); // Hemoglobin
        result2.setTestResult(testResults.get(1));
        result2.setIsReportable("Y");
        result2.setLastupdated(lastUpdated);
        result2.setMinNormal(13.5);
        result2.setMaxNormal(17.5);
        result2.setSignificantDigits(1);
        result2.setGrouping(1);
        result2.setFhirUuid(UUID.randomUUID());
        result2.setSysUserId("2");
        resultsList.add(result2);

        // Protein Result (Urinalysis)
        Result result3 = new Result();
        result3.setId("3");
        result3.setAnalysis(analyses.get(1)); // Urinalysis Analysis
        result3.setSortOrder("10");
        result3.setResultType("D");
        result3.setValue("NEG");
        result3.setAnalyte(analytes.get(3)); // Protein
        result3.setTestResult(testResults.get(2));
        result3.setIsReportable("Y");
        result3.setLastupdated(lastUpdated);
        result3.setMinNormal(null);
        result3.setMaxNormal(null);
        result3.setSignificantDigits(0);
        result3.setGrouping(1);
        result3.setFhirUuid(UUID.randomUUID());
        result3.setSysUserId("3");
        resultsList.add(result3);

        // Glucose Result
        Result result4 = new Result();
        result4.setId("4");
        result4.setAnalysis(analyses.get(2)); // Glucose Analysis
        result4.setSortOrder("10");
        result4.setResultType("N");
        result4.setValue("102");
        result4.setAnalyte(analytes.get(2)); // Glucose
        result4.setTestResult(testResults.get(4));
        result4.setIsReportable("Y");
        result4.setLastupdated(lastUpdated);
        result4.setMinNormal(70.0);
        result4.setMaxNormal(100.0);
        result4.setSignificantDigits(0);
        result4.setGrouping(1);
        result4.setFhirUuid(UUID.randomUUID());
        result4.setSysUserId("2");
        resultsList.add(result4);

        return resultsList;
    }

    private List<ResultSignature> createResultSignatures() {
        List<ResultSignature> resultSignaturesList = new ArrayList<>();

        ResultSignature sig1 = new ResultSignature();
        sig1.setId("1");
        sig1.setResultId("1");
        sig1.setSystemUserId("2"); // Jennifer Smith
        sig1.setIsSupervisor(false);
        sig1.setLastupdated(lastUpdated);
        sig1.setNonUserName("");
        resultSignaturesList.add(sig1);

        ResultSignature sig2 = new ResultSignature();
        sig2.setId("2");
        sig2.setResultId("2");
        sig2.setSystemUserId("1"); // Robert Johnson (supervisor)
        sig2.setIsSupervisor(true);
        sig2.setLastupdated(lastUpdated);
        sig2.setNonUserName("");
        resultSignaturesList.add(sig2);

        ResultSignature sig3 = new ResultSignature();
        sig3.setId("3");
        sig3.setResultId("4");
        sig3.setSystemUserId("3"); // Michael Williams
        sig3.setIsSupervisor(false);
        sig3.setLastupdated(lastUpdated);
        sig3.setNonUserName("");
        resultSignaturesList.add(sig3);

        return resultSignaturesList;
    }

    private List<TestReflex> createTestReflexes() {
        List<TestReflex> testReflexesList = new ArrayList<>();

        TestReflex reflex1 = new TestReflex();
        reflex1.setId("1");
        reflex1.setTestResultId("1");
        reflex1.setFlags("R");
        reflex1.setLastupdated(lastUpdated);
        reflex1.setTestAnalyteId("1");
        reflex1.setTestId("1");
        reflex1.setAddedTest(tests.get(2)); // Add Glucose test
        reflex1.setNonDictionaryValue("10.0");
        reflex1.setRelation(ReflexRuleOptions.NumericRelationOptions.GREATER_THAN);
        reflex1.setInternalNote("Reflex to glucose if WBC > 10.0");
        reflex1.setExternalNote("Additional glucose testing performed due to elevated WBC");
        reflex1.setIsActive("Y");
        testReflexesList.add(reflex1);

        TestReflex reflex2 = new TestReflex();
        reflex2.setId("2");
        reflex2.setTestResultId("4");
        reflex2.setFlags("R");
        reflex2.setLastupdated(lastUpdated);
        reflex2.setTestAnalyteId("4");
        reflex2.setTestId("2");
        reflex2.setAddedTest(tests.get(0)); // Add CBC test
        reflex2.setNonDictionaryValue("1+");
        reflex2.setRelation(ReflexRuleOptions.NumericRelationOptions.EQUALS);
        reflex2.setInternalNote("Reflex to proteinuria if protein 1+");
        reflex2.setExternalNote("Complete blood count added due to proteinuria");
        reflex2.setIsActive("Y");
        testReflexesList.add(reflex2);

        return testReflexesList;
    }

    private List<ReflexRule> createReflexRules() {
        List<ReflexRule> reflexRulesList = new ArrayList<>();

        ReflexRule rule1 = new ReflexRule();
        rule1.setId(1);
        rule1.setRuleName("High WBC Reflex");
        rule1.setOverall(ReflexRuleOptions.OverallOptions.ALL);
        rule1.setToggled(true);
        rule1.setActive(true);
        rule1.setAnalyteId(1);
        rule1.setLastupdated(lastUpdated);
        reflexRulesList.add(rule1);

        ReflexRule rule2 = new ReflexRule();
        rule2.setId(2);
        rule2.setRuleName("Proteinuria Reflex");
        rule2.setOverall(ReflexRuleOptions.OverallOptions.ANY);
        rule2.setToggled(true);
        rule2.setActive(true);
        rule2.setAnalyteId(4);
        rule2.setLastupdated(lastUpdated);
        reflexRulesList.add(rule2);

        return reflexRulesList;
    }

    private List<ReflexRuleCondition> createReflexRuleConditions() {
        List<ReflexRuleCondition> reflexRuleConditionsList = new ArrayList<>();

        ReflexRuleCondition condition1 = new ReflexRuleCondition();
        condition1.setId(1);
        condition1.setSampleId("1");
        condition1.setTestName("Complete Blood Count");
        condition1.setTestId("1");
        condition1.setRelation(ReflexRuleOptions.NumericRelationOptions.GREATER_THAN);
        condition1.setValue("10.0");
        condition1.setValue2(null);
        condition1.setTestAnalyteId(1);
        reflexRuleConditionsList.add(condition1);

        ReflexRuleCondition condition2 = new ReflexRuleCondition();
        condition2.setId(2);
        condition2.setSampleId("2");
        condition2.setTestName("Urinalysis");
        condition2.setTestId("2");
        condition2.setRelation(ReflexRuleOptions.NumericRelationOptions.EQUALS);
        condition2.setValue("1+");
        condition2.setValue2(null);
        condition2.setTestAnalyteId(4);
        reflexRuleConditionsList.add(condition2);

        return reflexRuleConditionsList;
    }

    private List<InventoryStorageLocation> createInventoryStorageLocations() {
        List<InventoryStorageLocation> inventoryStorageLocationsList = new ArrayList<>();

        InventoryStorageLocation location1 = new InventoryStorageLocation();
        location1.setId(1L);
        UUID uuid1 = UUID.randomUUID();
        location1.setFhirUuid(uuid1);
        location1.setName("Main Lab Storage");
        location1.setLocationCode("STOR-001");
        location1.setLocationType(InventoryEnums.LocationType.ROOM);
        location1.setDescription("Primary laboratory storage room");
        location1.setTemperatureMin(new BigDecimal("18.00"));
        location1.setTemperatureMax(new BigDecimal("24.00"));
        location1.setIsActive(true);
        location1.setLastupdated(lastUpdated);
        inventoryStorageLocationsList.add(location1);

        InventoryStorageLocation location2 = new InventoryStorageLocation();
        location2.setId(2L);
        location2.setFhirUuid(UUID.randomUUID());
        location2.setName("Chemistry Refrigerator");
        location2.setLocationCode("FRIDGE-101");
        location2.setLocationType(InventoryEnums.LocationType.REFRIGERATOR);
        location2.setDescription("Refrigerator for chemistry reagents");
        location2.setTemperatureMin(new BigDecimal("2.00"));
        location2.setTemperatureMax(new BigDecimal("8.00"));
        location2.setParentLocation(location1);
        location2.setIsActive(true);
        location2.setLastupdated(lastUpdated);
        inventoryStorageLocationsList.add(location2);

        InventoryStorageLocation location3 = new InventoryStorageLocation();
        location3.setId(3L);
        location3.setFhirUuid(UUID.randomUUID());
        location3.setName("Hematology Freezer");
        location3.setLocationCode("FREEZ-201");
        location3.setLocationType(InventoryEnums.LocationType.FREEZER);
        location3.setDescription("Freezer for hematology controls");
        location3.setTemperatureMin(new BigDecimal("-25.00"));
        location3.setTemperatureMax(new BigDecimal("-15.00"));
        location3.setParentLocation(location1);
        location3.setIsActive(true);
        location3.setLastupdated(lastUpdated);
        inventoryStorageLocationsList.add(location3);

        return inventoryStorageLocationsList;
    }

    private List<InventoryItem> createInventoryItems() {
        List<InventoryItem> inventoryItemsList = new ArrayList<>();

        InventoryItem item1 = new InventoryItem();
        item1.setId(1L);
        item1.setFhirUuid(UUID.randomUUID());
        item1.setName("CBC Reagent Kit");
        item1.setDescription("Reagent kit for complete blood count");
        item1.setItemType(InventoryEnums.ItemType.REAGENT);
        item1.setCategory("Hematology");
        item1.setManufacturer("Sysmex");
        item1.setCatalogNumber("CBC-100");
        item1.setStorageRequirements("2-8°C");
        item1.setQuantityPerUnit(1);
        item1.setUnits("kit");
        item1.setLowStockThreshold(2);
        item1.setExpirationAlertDays(30);
        item1.setStabilityAfterOpening(90);
        item1.setCalibrationRequired("Y");
        item1.setIndividualTracking("N");
        item1.setIsActive("Y");
        item1.setLastupdated(lastUpdated);
        inventoryItemsList.add(item1);

        InventoryItem item2 = new InventoryItem();
        item2.setId(2L);
        item2.setFhirUuid(UUID.randomUUID());
        item2.setName("Glucose Reagent");
        item2.setDescription("Glucose oxidase reagent");
        item2.setItemType(InventoryEnums.ItemType.REAGENT);
        item2.setCategory("Chemistry");
        item2.setManufacturer("Roche");
        item2.setCatalogNumber("GLU-200");
        item2.setStorageRequirements("2-8°C");
        item2.setQuantityPerUnit(100);
        item2.setUnits("mL");
        item2.setLowStockThreshold(10);
        item2.setExpirationAlertDays(60);
        item2.setStabilityAfterOpening(30);
        item2.setCalibrationRequired("Y");
        item2.setIndividualTracking("N");
        item2.setIsActive("Y");
        item2.setLastupdated(lastUpdated);
        inventoryItemsList.add(item2);

        InventoryItem item3 = new InventoryItem();
        item3.setId(3L);
        item3.setFhirUuid(UUID.randomUUID());
        item3.setName("Urinalysis Strips");
        item3.setDescription("10-parameter urinalysis strips");
        item3.setItemType(InventoryEnums.ItemType.REAGENT);
        item3.setCategory("Urinalysis");
        item3.setManufacturer("Siemens");
        item3.setCatalogNumber("UA-300");
        item3.setStorageRequirements("Room temperature");
        item3.setQuantityPerUnit(100);
        item3.setUnits("strips");
        item3.setLowStockThreshold(5);
        item3.setExpirationAlertDays(90);
        item3.setStabilityAfterOpening(60);
        item3.setCalibrationRequired("N");
        item3.setIndividualTracking("N");
        item3.setIsActive("Y");
        item3.setLastupdated(lastUpdated);
        inventoryItemsList.add(item3);

        return inventoryItemsList;
    }

    private List<ResultInventory> createResultInventories() {
        List<ResultInventory> resultInventoriesList = new ArrayList<>();

        ResultInventory ri1 = new ResultInventory();
        ri1.setId("1");
        ri1.setInventoryLocationId("2");
        ri1.setResultId("1");
        ri1.setDescription("WBC testing reagent used");
        ri1.setLastupdated(lastUpdated);
        resultInventoriesList.add(ri1);

        ResultInventory ri2 = new ResultInventory();
        ri2.setId("2");
        ri2.setInventoryLocationId("2");
        ri2.setResultId("4");
        ri2.setDescription("Glucose reagent used");
        ri2.setLastupdated(lastUpdated);
        resultInventoriesList.add(ri2);

        return resultInventoriesList;
    }

    private List<PatientIdentityType> createPatientIdentityTypes() {
        List<PatientIdentityType> patientIdentityTypesList = new ArrayList<>();

        PatientIdentityType type1 = new PatientIdentityType();
        type1.setId("1");
        type1.setIdentityType("ST");
        type1.setDescription("Subject Number");
        type1.setLastupdated(lastUpdated);
        patientIdentityTypesList.add(type1);

        PatientIdentityType type2 = new PatientIdentityType();
        type2.setId("2");
        type2.setIdentityType("ST_NUMBER");
        type2.setDescription("National ID");
        type2.setLastupdated(lastUpdated);
        patientIdentityTypesList.add(type2);

        PatientIdentityType type3 = new PatientIdentityType();
        type3.setId("3");
        type3.setIdentityType("PC_NUMBER");
        type3.setDescription("Health Insurance Number");
        type3.setLastupdated(lastUpdated);
        patientIdentityTypesList.add(type3);

        return patientIdentityTypesList;
    }

    private List<Person> createPersons() {
        List<Person> personsList = new ArrayList<>();

        Person person1 = new Person();
        person1.setId("1");
        person1.setLastName("Doe");
        person1.setFirstName("John");
        person1.setMiddleName("Robert");
        person1.setCity("Metropolis");
        person1.setState("CA");
        person1.setZipCode("12345");
        person1.setCountry("USA");
        person1.setWorkPhone("555-0101");
        person1.setHomePhone("555-0102");
        person1.setCellPhone("555-0103");
        person1.setPrimaryPhone("555-0103");
        person1.setFax("555-0104");
        person1.setEmail("john.doe@email.com");
        person1.setLastupdated(lastUpdated);
        personsList.add(person1);

        Person person2 = new Person();
        person2.setId("2");
        person2.setLastName("Smith");
        person2.setFirstName("Jane");
        person2.setMiddleName("Elizabeth");
        person2.setCity("Centerville");
        person2.setState("NY");
        person2.setZipCode("67890");
        person2.setCountry("USA");
        person2.setWorkPhone("555-0201");
        person2.setHomePhone("555-0202");
        person2.setCellPhone("555-0203");
        person2.setPrimaryPhone("555-0203");
        person2.setFax("555-0204");
        person2.setEmail("jane.smith@email.com");
        person2.setLastupdated(lastUpdated);
        personsList.add(person2);

        Person person3 = new Person();
        person3.setId("3");
        person3.setLastName("Johnson");
        person3.setFirstName("Michael");
        person3.setMiddleName("David");
        person3.setCity("Springfield");
        person3.setState("IL");
        person3.setZipCode("54321");
        person3.setCountry("USA");
        person3.setWorkPhone("555-0301");
        person3.setHomePhone("555-0302");
        person3.setCellPhone("555-0303");
        person3.setPrimaryPhone("555-0303");
        person3.setFax("555-0304");
        person3.setEmail("michael.johnson@email.com");
        person3.setLastupdated(lastUpdated);
        personsList.add(person3);

        return personsList;
    }

    private List<Patient> createPatients() {
        List<Patient> patientsList = new ArrayList<>();

        Patient patient1 = new Patient();
        patient1.setId("1");
        patient1.setPerson(persons.get(0));
        patient1.setRace("Caucasian");
        patient1.setGender("M");
        Timestamp birthDate = Timestamp.valueOf("1980-05-15 00:00:00");
        patient1.setBirthDate(birthDate);
        Date birthTime = new Date(birthDate.getTime());
        patient1.setBirthTime(birthTime);
        patient1.setNationalId("NID001");
        patient1.setEthnicity("Not Hispanic");
        patient1.setExternalId("EXT001");
        patient1.setSchoolAttend("Metropolis University");
        patient1.setBirthPlace("Metropolis General Hospital");
        patient1.setLastupdated(lastUpdated);
        patient1.setSysUserId("1");
        patientsList.add(patient1);

        Patient patient2 = new Patient();
        patient2.setId("2");
        patient2.setPerson(persons.get(1));
        patient2.setRace("African American");
        patient2.setGender("F");
        Timestamp birthDate2 = Timestamp.valueOf("1990-08-22 00:00:00");
        Date birthTime2 = new Date(birthDate2.getTime());
        patient2.setBirthDate(birthDate2);
        patient2.setBirthTime(birthTime2);
        patient2.setNationalId("NID002");
        patient2.setEthnicity("Not Hispanic");
        patient2.setExternalId("EXT002");
        patient2.setSchoolAttend("Centerville College");
        patient2.setBirthPlace("Centerville Medical Center");
        patient2.setLastupdated(lastUpdated);
        patient2.setSysUserId("1");
        patientsList.add(patient2);

        Patient patient3 = new Patient();
        patient3.setId("3");
        patient3.setPerson(persons.get(2));
        patient3.setRace("Asian");
        patient3.setGender("M");
        Timestamp birthDate3 = Timestamp.valueOf("1975-11-30 00:00:00");
        Date birthTime3 = new Date(birthDate3.getTime());
        patient3.setBirthDate(birthDate3);
        patient3.setBirthTime(birthTime3);
        patient3.setNationalId("NID003");
        patient3.setEthnicity("Not Hispanic");
        patient3.setExternalId("EXT003");
        patient3.setSchoolAttend("Springfield Technical Institute");
        patient3.setBirthPlace("Springfield Hospital");
        patient3.setLastupdated(lastUpdated);
        patient3.setSysUserId("1");
        patientsList.add(patient3);

        return patientsList;
    }

    // Getters for all lists - now return initialized lists
    public List<Localization> getLocalizations() {
        return localizations;
    }

    public List<SystemUser> getSystemUsers() {
        return systemUsers;
    }

    public List<UnitOfMeasure> getUnitOfMeasures() {
        return unitOfMeasures;
    }

    public List<Report> getReports() {
        return reports;
    }

    public List<TestTrailer> getTestTrailers() {
        return testTrailers;
    }

    public List<Scriptlet> getScriptlets() {
        return scriptlets;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public List<StatusOfSample> getStatusOfSamples() {
        return statusOfSamples;
    }

    public List<TypeOfSample> getTypeOfSamples() {
        return typeOfSamples;
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public List<SampleItem> getSampleItems() {
        return sampleItems;
    }

    public List<Organization> getOrganizations() {
        return organizations;
    }

    public List<TestSection> getTestSections() {
        return testSections;
    }

    public List<Test> getTests() {
        return tests;
    }

    public List<TestResult> getTestResults() {
        return testResults;
    }

    public List<Analyte> getAnalytes() {
        return analytes;
    }

    public List<TestAnalyte> getTestAnalytes() {
        return testAnalytes;
    }

    public List<Panel> getPanels() {
        return panels;
    }

    public List<Analysis> getAnalyses() {
        return analyses;
    }

    public List<Result> getResults() {
        return results;
    }

    public List<ResultSignature> getResultSignatures() {
        return resultSignatures;
    }

    public List<TestReflex> getTestReflexes() {
        return testReflexes;
    }

    public List<ReflexRule> getReflexRules() {
        return reflexRules;
    }

    public List<ReflexRuleCondition> getReflexRuleConditions() {
        return reflexRuleConditions;
    }

    public List<InventoryStorageLocation> getInventoryStorageLocations() {
        return inventoryStorageLocations;
    }

    public List<InventoryItem> getInventoryItems() {
        return inventoryItems;
    }

    public List<ResultInventory> getResultInventories() {
        return resultInventories;
    }

    public List<PatientIdentityType> getPatientIdentityTypes() {
        return patientIdentityTypes;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public List<Patient> getPatients() {
        return patients;
    }
}