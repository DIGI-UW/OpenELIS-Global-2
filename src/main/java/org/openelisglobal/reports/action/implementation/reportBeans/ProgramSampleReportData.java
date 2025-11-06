package org.openelisglobal.reports.action.implementation.reportBeans;

import java.util.List;

public final class ProgramSampleReportData {

    private String patientName = "";

    private String nationalId;

    private String gender;

    private String dob;

    private String age;

    private String stNumber;

    private String subjectNumber;

    private String contactInfo;

    private String siteInfo;

    private String testName;

    private String testRefRange;

    private String conclusion;

    private String finishDate;

    private String accessionNumber;

    private String receivedDate;

    private String testDate;

    private String referralSentDate;

    private String referralTestName;

    private String referralResult;

    private String referralResultReportDate;

    private String referralReason;

    private String referralRefRange;

    private String referralNote;

    private String firstName = "";

    private String lastName = "";

    private String dept;

    private String commune;

    private String healthDistrict = "";

    private String healthRegion = "";

    private int sectionSortOrder = 0;

    private int testSortOrder = 0;

    private String orderDate;

    private String patientSiteNumber;

    private boolean parentMarker = false;

    private String billingNumber;

    private String sampleType;

    private String sampleId;

    private String sampleSortOrder;

    private String analysisStatus;

    private String contactTracingIndexName;

    private String contactTracingIndexRecordNumber;

    private String completeFlag;

    private String orderFinishDate;

    private String collectionDateTime;

    private boolean correctedResult = false;

    private String labOrderType = "";

    // Pathology data
    private String grossExam;

    private String microExam;

    private List<String> codedConclusion;

    private String textConclusion;

    // Cytology data
    private String specimenAdequacy;

    private String diagnosis;

    private List<String> nonNeoplasticCellularVariations;

    private List<String> reactiveCellularChanges;

    private List<String> organisms;

    private List<String> epithelialCellAbnomalities;

    private List<String> otherDiagnoses;

    // ImmunoHistoChemistry data
    private List<Result> results;

    private String erResult;

    private String prResult;

    private String mibResult;

    private String ihcReportDetails;

    private String ihcScore;

    public ProgramSampleReportData() {
    }

    public String getReferralRefRange() {
        return referralRefRange;
    }

    public void setReferralRefRange(String referralRefRange) {
        this.referralRefRange = referralRefRange;
    }

    public String getTestRefRange() {
        return testRefRange;
    }

    public void setTestRefRange(String testRefRange) {
        this.testRefRange = testRefRange;
    }

    public String getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(String finishDate) {
        this.finishDate = finishDate;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getStNumber() {
        return stNumber;
    }

    public void setStNumber(String stNumber) {
        this.stNumber = stNumber;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestName() {
        return testName;
    }

    public void setReceivedDate(String recievedDate) {
        this.receivedDate = recievedDate;
    }

    // in case of typo
    public String getRecievedDate() {
        return getReceivedDate();
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setConclusion(String conclusioned) {
        conclusion = conclusioned;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setSiteInfo(String siteInfo) {
        this.siteInfo = siteInfo;
    }

    public String getSiteInfo() {
        return siteInfo;
    }

    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }

    public String getTestDate() {
        return testDate;
    }

    public String getReferralSentDate() {
        return referralSentDate;
    }

    public void setReferralSentDate(String referralSentDate) {
        this.referralSentDate = referralSentDate;
    }

    public String getReferralTestName() {
        return referralTestName;
    }

    public void setReferralTestName(String referralTestName) {
        this.referralTestName = referralTestName;
    }

    public String getReferralResult() {
        return referralResult;
    }

    public void setReferralResult(String referralResult) {
        this.referralResult = referralResult;
    }

    public void setReferralResultReportDate(String referralResultReportDate) {
        this.referralResultReportDate = referralResultReportDate;
    }

    public String getReferralResultReportDate() {
        return referralResultReportDate;
    }

    public void setReferralReason(String referralReason) {
        this.referralReason = referralReason;
    }

    public String getReferralReason() {
        return referralReason;
    }

    public String getReferralNote() {
        return referralNote;
    }

    public void setReferralNote(String referralNote) {
        this.referralNote = referralNote;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSampleSortOrder() {
        return sampleSortOrder;
    }

    public void setSampleSortOrder(String sampleSortOrder) {
        this.sampleSortOrder = sampleSortOrder;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getCommune() {
        return commune;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public int getTestSortOrder() {
        return testSortOrder;
    }

    public void setTestSortOrder(int testSortOrder) {
        this.testSortOrder = testSortOrder;
    }

    public int getSectionSortOrder() {
        return sectionSortOrder;
    }

    public void setSectionSortOrder(int sectionSortOrder) {
        this.sectionSortOrder = sectionSortOrder;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getHealthDistrict() {
        return healthDistrict;
    }

    public void setHealthDistrict(String healthDistrict) {
        this.healthDistrict = healthDistrict;
    }

    public String getHealthRegion() {
        return healthRegion;
    }

    public void setHealthRegion(String healthRegion) {
        this.healthRegion = healthRegion;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getPatientSiteNumber() {
        return patientSiteNumber;
    }

    public void setPatientSiteNumber(String patientSiteNumber) {
        this.patientSiteNumber = patientSiteNumber;
    }

    public boolean getParentMarker() {
        return parentMarker;
    }

    public void setParentMarker(boolean isParentMarker) {
        this.parentMarker = isParentMarker;
    }

    public String getBillingNumber() {
        return billingNumber;
    }

    public void setBillingNumber(String billingNumber) {
        this.billingNumber = billingNumber;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public String getContactTracingIndexName() {
        return contactTracingIndexName;
    }

    public void setContactTracingIndexName(String contactTracingIndexName) {
        this.contactTracingIndexName = contactTracingIndexName;
    }

    public String getContactTracingIndexRecordNumber() {
        return contactTracingIndexRecordNumber;
    }

    public void setContactTracingIndexRecordNumber(String contactTracingIndexRecordNumber) {
        this.contactTracingIndexRecordNumber = contactTracingIndexRecordNumber;
    }

    public String getCompleteFlag() {
        return completeFlag;
    }

    public void setCompleteFlag(String completeFlag) {
        this.completeFlag = completeFlag;
    }

    public String getOrderFinishDate() {
        return orderFinishDate;
    }

    public void setOrderFinishDate(String orderFinishDate) {
        this.orderFinishDate = orderFinishDate;
    }

    public String getCollectionDateTime() {
        return collectionDateTime;
    }

    public void setCollectionDateTime(String collectionDateTime) {
        this.collectionDateTime = collectionDateTime;
    }

    public boolean getCorrectedResult() {
        return correctedResult;
    }

    public void setCorrectedResult(boolean correctedResult) {
        this.correctedResult = correctedResult;
    }

    public String getLabOrderType() {
        return labOrderType;
    }

    public void setLabOrderType(String labOrderType) {
        this.labOrderType = labOrderType;
    }

    public String getGrossExam() {
        return grossExam;
    }

    public void setGrossExam(String grossExam) {
        this.grossExam = grossExam;
    }

    public String getMicroExam() {
        return microExam;
    }

    public void setMicroExam(String microExam) {
        this.microExam = microExam;
    }

    public List<String> getCodedConclusion() {
        return codedConclusion;
    }

    public void setCodedConclusion(List<String> codedConclusion) {
        this.codedConclusion = codedConclusion;
    }

    public String getTextConclusion() {
        return textConclusion;
    }

    public void setTextConclusion(String textConclusion) {
        this.textConclusion = textConclusion;
    }

    public String getSpecimenAdequacy() {
        return specimenAdequacy;
    }

    public void setSpecimenAdequacy(String specimenAdequacy) {
        this.specimenAdequacy = specimenAdequacy;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public List<String> getNonNeoplasticCellularVariations() {
        return nonNeoplasticCellularVariations;
    }

    public void setNonNeoplasticCellularVariations(List<String> nonNeoplasticCellularVariations) {
        this.nonNeoplasticCellularVariations = nonNeoplasticCellularVariations;
    }

    public List<String> getReactiveCellularChanges() {
        return reactiveCellularChanges;
    }

    public void setReactiveCellularChanges(List<String> reactiveCellularChanges) {
        this.reactiveCellularChanges = reactiveCellularChanges;
    }

    public List<String> getOrganisms() {
        return organisms;
    }

    public void setOrganisms(List<String> organisms) {
        this.organisms = organisms;
    }

    public List<String> getEpithelialCellAbnomalities() {
        return epithelialCellAbnomalities;
    }

    public void setEpithelialCellAbnomalities(List<String> epithelialCellAbnomalities) {
        this.epithelialCellAbnomalities = epithelialCellAbnomalities;
    }

    public List<String> getOtherDiagnoses() {
        return otherDiagnoses;
    }

    public void setOtherDiagnoses(List<String> otherDiagnoses) {
        this.otherDiagnoses = otherDiagnoses;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public String getErResult() {
        return erResult;
    }

    public void setErResult(String erResult) {
        this.erResult = erResult;
    }

    public String getPrResult() {
        return prResult;
    }

    public void setPrResult(String prResult) {
        this.prResult = prResult;
    }

    public String getMibResult() {
        return mibResult;
    }

    public void setMibResult(String mibResult) {
        this.mibResult = mibResult;
    }

    public String getIhcReportDetails() {
        return ihcReportDetails;
    }

    public void setIhcReportDetails(String ihcReportDetails) {
        this.ihcReportDetails = ihcReportDetails;
    }

    public String getIhcScore() {
        return ihcScore;
    }

    public void setIhcScore(String ihcScore) {
        this.ihcScore = ihcScore;
    }

    public static class Result {

        private String result;

        private String test;

        private String uom;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public String getUom() {
            return uom;
        }

        public void setUom(String uom) {
            this.uom = uom;
        }
    }
}
