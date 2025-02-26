package org.openelisglobal.sample.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.validation.annotations.SafeHtml;

public class ProjectData implements Serializable {

    public ProjectData() {
    }

    private static final long serialVersionUID = -6470190207790723782L;
    // ALL
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String underInvestigationNote;

    // TESTS
    private boolean dryTubeTaken;
    private boolean serologyHIVTest;
    private boolean murexTest;
    private boolean integralTest;
    private boolean genscreenTest;
    private boolean genieIITest;
    private boolean vironostikaTest;
    private boolean genieII100Test;
    private boolean genieII10Test;
    private boolean WB1Test;
    private boolean WB2Test;
    private boolean p24AgTest;
    private boolean glycemiaTest;
    private boolean creatinineTest;
    private boolean transaminaseTest;
    private boolean transaminaseALTLTest;
    private boolean transaminaseASTLTest;
    private boolean edtaTubeTaken;
    private boolean nfsTest;
    private boolean gbTest;
    private boolean neutTest;
    private boolean lymphTest;
    private boolean monoTest;
    private boolean eoTest;
    private boolean basoTest;
    private boolean grTest;
    private boolean hbTest;
    private boolean hctTest;
    private boolean vgmTest;
    private boolean tcmhTest;
    private boolean ccmhTest;
    private boolean plqTest;
    private boolean cd4cd8Test;
    private boolean cd4CountTest;
    private boolean cd3CountTest;
    private boolean innoliaTest;
    private boolean viralLoadTest;
    private boolean hpvTest;
    private boolean genotypingTest;
    private boolean dnaPCR;
    private boolean dbsTaken;
    private boolean dbsvlTaken;
    private boolean pscvlTaken;
    private boolean asanteTest;
    private boolean plasmaTaken;
    private boolean serumTaken;
    private boolean preservCytTaken = true; // for HPV sample
    private boolean abbottOrRocheAnalysis = true; // for HPV sample
    private boolean geneXpertAnalysis; // for HPV sample
    private boolean selfCollection; // for HPV sample collection
    private boolean collectionDoneByHealthWorker; // for HPV sample collection

    // ARV
    private String doctor;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String ARVcenterName;

    private List ARVCenters;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String ARVcenterCode;

    // EID
    private List EIDSites;
    private List<Organization> EIDSitesByName;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String EIDsiteName;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String EIDsiteCode;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dbsInfantNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dbsSiteInfantNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String eidWhichPCR;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String eidSecondPCRReason;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String requester;

    private List eidWhichPCRList = new ArrayList();
    private List eidSecondPCRReasonList = new ArrayList();
    private List isUnderInvestigationList = new ArrayList();

    // IND - Indeterminate Results
    private List INDSites;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String INDsiteName;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String INDsiteCode;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String address;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    private String phoneNumber;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    private String faxNumber;

    @Email
    private String email;

    // SPE - Special Request
    private List requestReasons;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reasonForRequest;

    // TESTS
    public void setDbsTaken(boolean dbsTaken) {
        this.dbsTaken = dbsTaken;
    }

    public boolean getDbsTaken() {
        return dbsTaken;
    }

    public boolean getDryTubeTaken() {
        return dryTubeTaken;
    }

    public void setDryTubeTaken(boolean dryTubeTaken) {
        this.dryTubeTaken = dryTubeTaken;
    }

    public boolean getSerologyHIVTest() {
        return serologyHIVTest;
    }

    public void setSerologyHIVTest(boolean serologyHIVTest) {
        this.serologyHIVTest = serologyHIVTest;
    }

    public boolean getMurexTest() {
        return murexTest;
    }

    public void setMurexTest(boolean murexTest) {
        this.murexTest = murexTest;
    }

    public boolean getIntegralTest() {
        return integralTest;
    }

    public void setIntegralTest(boolean integralTest) {
        this.integralTest = integralTest;
    }

    public boolean getGenieIITest() {
        return genieIITest;
    }

    public void setGenieIITest(boolean genieIITest) {
        this.genieIITest = genieIITest;
    }

    public boolean getVironostikaTest() {
        return vironostikaTest;
    }

    public void setVironostikaTest(boolean vironostikaTest) {
        this.vironostikaTest = vironostikaTest;
    }

    public boolean getGenieII100Test() {
        return genieII100Test;
    }

    public void setGenieII100Test(boolean genieII100Test) {
        this.genieII100Test = genieII100Test;
    }

    public boolean getGenieII10Test() {
        return genieII10Test;
    }

    public void setGenieII10Test(boolean genieII10Test) {
        this.genieII10Test = genieII10Test;
    }

    public boolean getWB1Test() {
        return WB1Test;
    }

    public void setWB1Test(boolean wB1Test) {
        WB1Test = wB1Test;
    }

    public boolean getWB2Test() {
        return WB2Test;
    }

    public void setWB2Test(boolean wB2Test) {
        WB2Test = wB2Test;
    }

    public boolean getP24AgTest() {
        return p24AgTest;
    }

    public void setP24AgTest(boolean p24AgTest) {
        this.p24AgTest = p24AgTest;
    }

    public boolean getGlycemiaTest() {
        return glycemiaTest;
    }

    public void setGlycemiaTest(boolean glycemiaTest) {
        this.glycemiaTest = glycemiaTest;
    }

    public boolean getCreatinineTest() {
        return creatinineTest;
    }

    public void setCreatinineTest(boolean creatinineTest) {
        this.creatinineTest = creatinineTest;
    }

    public boolean getTransaminaseTest() {
        return transaminaseTest;
    }

    public void setTransaminaseTest(boolean transaminaseTest) {
        this.transaminaseTest = transaminaseTest;
    }

    public boolean getTransaminaseALTLTest() {
        return transaminaseALTLTest;
    }

    public void setTransaminaseALTLTest(boolean transaminaseALTLTest) {
        this.transaminaseALTLTest = transaminaseALTLTest;
    }

    public boolean getTransaminaseASTLTest() {
        return transaminaseASTLTest;
    }

    public void setTransaminaseASTLTest(boolean transaminaseASTLTest) {
        this.transaminaseASTLTest = transaminaseASTLTest;
    }

    public boolean getEdtaTubeTaken() {
        return edtaTubeTaken;
    }

    public void setEdtaTubeTaken(boolean edtaTubeTaken) {
        this.edtaTubeTaken = edtaTubeTaken;
    }

    public boolean getNfsTest() {
        return nfsTest;
    }

    public void setNfsTest(boolean nfsTest) {
        this.nfsTest = nfsTest;
    }

    public boolean getGbTest() {
        return gbTest;
    }

    public void setGbTest(boolean gbTest) {
        this.gbTest = gbTest;
    }

    public boolean getNeutTest() {
        return neutTest;
    }

    public void setNeutTest(boolean neutTest) {
        this.neutTest = neutTest;
    }

    public boolean getLymphTest() {
        return lymphTest;
    }

    public void setLymphTest(boolean lymphTest) {
        this.lymphTest = lymphTest;
    }

    public boolean getMonoTest() {
        return monoTest;
    }

    public void setMonoTest(boolean monoTest) {
        this.monoTest = monoTest;
    }

    public boolean getEoTest() {
        return eoTest;
    }

    public void setEoTest(boolean eoTest) {
        this.eoTest = eoTest;
    }

    public boolean getBasoTest() {
        return basoTest;
    }

    public void setBasoTest(boolean basoTest) {
        this.basoTest = basoTest;
    }

    public boolean getGrTest() {
        return grTest;
    }

    public void setGrTest(boolean grTest) {
        this.grTest = grTest;
    }

    public boolean getHbTest() {
        return hbTest;
    }

    public void setHbTest(boolean hbTest) {
        this.hbTest = hbTest;
    }

    public boolean getHctTest() {
        return hctTest;
    }

    public void setHctTest(boolean hctTest) {
        this.hctTest = hctTest;
    }

    public boolean getVgmTest() {
        return vgmTest;
    }

    public void setVgmTest(boolean vgmTest) {
        this.vgmTest = vgmTest;
    }

    public boolean getTcmhTest() {
        return tcmhTest;
    }

    public void setTcmhTest(boolean tcmhTest) {
        this.tcmhTest = tcmhTest;
    }

    public boolean getCcmhTest() {
        return ccmhTest;
    }

    public void setCcmhTest(boolean ccmhTest) {
        this.ccmhTest = ccmhTest;
    }

    public boolean getPlqTest() {
        return plqTest;
    }

    public void setPlqTest(boolean plqTest) {
        this.plqTest = plqTest;
    }

    public boolean getCd4cd8Test() {
        return cd4cd8Test;
    }

    public void setCd4cd8Test(boolean cd4cd8Test) {
        this.cd4cd8Test = cd4cd8Test;
    }

    public boolean getCd4CountTest() {
        return cd4CountTest;
    }

    public void setCd4CountTest(boolean cd4CountTest) {
        this.cd4CountTest = cd4CountTest;
    }

    public boolean getCd3CountTest() {
        return cd3CountTest;
    }

    public void setCd3CountTest(boolean cd3CountTest) {
        this.cd3CountTest = cd3CountTest;
    }

    public boolean getViralLoadTest() {
        return viralLoadTest;
    }

    public void setViralLoadTest(boolean viralLoadTest) {
        this.viralLoadTest = viralLoadTest;
    }

    // -------
    public boolean getdbsvlTaken() {
        return dbsvlTaken;
    }

    public void setdbsvlTaken(boolean dbsvlTaken) {
        this.dbsvlTaken = dbsvlTaken;
    }

    // ------
    public boolean getGenotypingTest() {
        return genotypingTest;
    }

    public void setGenotypingTest(boolean genotypingTest) {
        this.genotypingTest = genotypingTest;
    }

    public boolean getDnaPCR() {
        return dnaPCR;
    }

    public void setDnaPCR(boolean dnaPCR) {
        this.dnaPCR = dnaPCR;
    }

    // ARV

    public List getARVCenters() {
        return ARVCenters;
    }

    public void setARVCenters(List arvCenters) {
        ARVCenters = arvCenters;
    }

    public String getARVcenterName() {
        return ARVcenterName;
    }

    public void setARVcenterName(String aRVcenterName) {
        ARVcenterName = aRVcenterName;
    }

    public String getARVcenterCode() {
        return ARVcenterCode;
    }

    public void setARVcenterCode(String aRVcenterCode) {
        ARVcenterCode = aRVcenterCode;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    // EID
    public String getEIDSiteName() {
        return EIDsiteName;
    }

    public void setEIDSiteName(String siteName) {
        EIDsiteName = siteName;
    }

    public List getEIDSites() {
        return EIDSites;
    }

    public void setEIDSites(List eIDSites) {
        EIDSites = eIDSites;
    }

    public String getEIDsiteCode() {
        return EIDsiteCode;
    }

    public void setEIDsiteCode(String eIDsiteCode) {
        EIDsiteCode = eIDsiteCode;
    }

    public String getDbsInfantNumber() {
        return dbsInfantNumber;
    }

    public void setDbsInfantNumber(String dbsInfantNumber) {
        this.dbsInfantNumber = dbsInfantNumber;
    }

    public String getDbsSiteInfantNumber() {
        return dbsSiteInfantNumber;
    }

    public void setDbsSiteInfantNumber(String dbsSiteInfantNumber) {
        this.dbsSiteInfantNumber = dbsSiteInfantNumber;
    }

    public String getEidWhichPCR() {
        return eidWhichPCR;
    }

    public void setEidWhichPCR(String eidWhichPCR) {
        this.eidWhichPCR = eidWhichPCR;
    }

    public String getEidSecondPCRReason() {
        return eidSecondPCRReason;
    }

    public void setEidSecondPCRReason(String eidSecondPCRReason) {
        this.eidSecondPCRReason = eidSecondPCRReason;
    }

    public void setEidWhichPCRList(List eidWhichPCRList) {
        this.eidWhichPCRList = eidWhichPCRList;
    }

    public List getEidWhichPCRList() {
        return eidWhichPCRList;
    }

    public void setEidSecondPCRReasonList(List eidSecondPCRReasonList) {
        this.eidSecondPCRReasonList = eidSecondPCRReasonList;
    }

    public List getEidSecondPCRReasonList() {
        return eidSecondPCRReasonList;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    // IND
    public List getINDSites() {
        return INDSites;
    }

    public void setINDSites(List iNDSites) {
        INDSites = iNDSites;
    }

    public String getINDsiteName() {
        return INDsiteName;
    }

    public void setINDsiteName(String iNDsiteName) {
        INDsiteName = iNDsiteName;
    }

    public String getINDsiteCode() {
        return INDsiteCode;
    }

    public void setINDsiteCode(String iNDsiteCode) {
        INDsiteCode = iNDsiteCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // SPE
    public List getRequestReasons() {
        return requestReasons;
    }

    public void setRequestReasons(List requestReasons) {
        this.requestReasons = requestReasons;
    }

    public String getReasonForRequest() {
        return reasonForRequest;
    }

    public void setReasonForRequest(String reasonForRequest) {
        this.reasonForRequest = reasonForRequest;
    }

    public void setIsUnderInvestigationList(List isUnderInvestigationList) {
        this.isUnderInvestigationList = isUnderInvestigationList;
    }

    public List getIsUnderInvestigationList() {
        return isUnderInvestigationList;
    }

    public void setEIDSitesByName(List<Organization> eIDSitesByName) {
        EIDSitesByName = eIDSitesByName;
    }

    public List<Organization> getEIDSitesByName() {
        return EIDSitesByName;
    }

    public List<Dictionary> getHivStatusList() {
        return ObservationHistoryList.HIV_STATUSES.getList();
    }

    public void setUnderInvestigationNote(String underInvestigationNote) {
        this.underInvestigationNote = underInvestigationNote;
    }

    public String getUnderInvestigationNote() {
        return underInvestigationNote;
    }

    public boolean getInnoliaTest() {
        return innoliaTest;
    }

    public void setInnoliaTest(boolean innoliaTest) {
        this.innoliaTest = innoliaTest;
    }

    public boolean isPscvlTaken() {
        return pscvlTaken;
    }

    public void setPscvlTaken(boolean pscvlTaken) {
        this.pscvlTaken = pscvlTaken;
    }

    public boolean isDbsvlTaken() {
        return dbsvlTaken;
    }

    public boolean isPlasmaTaken() {
        return plasmaTaken;
    }

    public void setPlasmaTaken(boolean plasmaTaken) {
        this.plasmaTaken = plasmaTaken;
    }

    public boolean isSerumTaken() {
        return serumTaken;
    }

    public void setSerumTaken(boolean serumTaken) {
        this.serumTaken = serumTaken;
    }

    public boolean isAsanteTest() {
        return asanteTest;
    }

    public void setAsanteTest(boolean asanteTest) {
        this.asanteTest = asanteTest;
    }

    public boolean getGenscreenTest() {
        return genscreenTest;
    }

    public void setGenscreenTest(boolean genscreenTest) {
        this.genscreenTest = genscreenTest;
    }

    public boolean isHpvTest() {
        return hpvTest;
    }

    public void setHpvTest(boolean hpvTest) {
        this.hpvTest = hpvTest;
    }

    public boolean getSelfCollection() {
        return selfCollection;
    }

    public void setSelfCollection(boolean selfCollection) {
        this.selfCollection = selfCollection;
    }

    public boolean getCollectionDoneByHealthWorker() {
        return collectionDoneByHealthWorker;
    }

    public void setCollectionDoneByHealthWorker(boolean collectionDoneByHealthWorker) {
        this.collectionDoneByHealthWorker = collectionDoneByHealthWorker;
    }

    public boolean isAbbottOrRocheAnalysis() {
        return abbottOrRocheAnalysis;
    }

    public void setAbbottOrRocheAnalysis(boolean abottOrRocheAnalysis) {
        this.abbottOrRocheAnalysis = abottOrRocheAnalysis;
    }

    public boolean isGeneXpertAnalysis() {
        return geneXpertAnalysis;
    }

    public void setGeneXpertAnalysis(boolean geneXpertAnalysis) {
        this.geneXpertAnalysis = geneXpertAnalysis;
    }

    public boolean isPreservCytTaken() {
        return preservCytTaken;
    }

    public void setPreservCytTaken(boolean preservCytTaken) {
        this.preservCytTaken = preservCytTaken;
    }
}
