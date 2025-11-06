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
package org.openelisglobal.patient.valueholder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.resultvalidation.form.ResultValidationForm;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

/**
 * Value object for transfer of observation history (demographic survey
 * questions) from the study (project) based patient entry form. This is one
 * object for all studies.
 *
 * @author pahill
 * @since 2010-04-16
 */
public class ObservationData implements Serializable {
    private static final long serialVersionUID = 2L;

    public ObservationData() {
        super();
    }

    /**
     * General tag which identifies which set of questions have been answered to
     * enter/accession this person and sample into the system.
     */
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectFormName;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String educationLevel;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String maritalStatus;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String nationality;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nationalityOther;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String legalResidence;

    @ValidName(nameType = NameType.FULL_NAME)
    private String nameOfDoctor;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String arvProphylaxisBenefit;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String arvProphylaxis;

    /**
     * Used for both Initial ARV CLI 04 "currently under ARV treatment" and the
     * slightly different question Follow-up ARV SUI 20 "... on going ARV treatment
     * ..."
     */
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String currentARVTreatment;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String priorARVTreatment;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String interruptedARVTreatment;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String aidsStage;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String hivStatus;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String anyPriorDiseases;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String priorDiseases; // drop down value yes, no

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String priorDiseasesValue; // actual string containing other Diseases which is answer to "please
    // specify"

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String anyCurrentDiseases;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String currentDiseases;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String currentDiseasesValue; // actual string containing other Diseases which is answer to "please
    // specify"

    private List<Pair<String, String>> priorDiseasesList;
    private List<Pair<String, String>> currentDiseasesList;

    private List<Pair<String, String>> rtnPriorDiseasesList;
    private List<Pair<String, String>> rtnCurrentDiseasesList;

    private List<@SafeHtml(level = SafeHtml.SafeListLevel.NONE) String> priorARVTreatmentINNs = Arrays
            .asList(new String[] { null, null, null, null });
    private List<@SafeHtml(level = SafeHtml.SafeListLevel.NONE) String> futureARVTreatmentINNs = Arrays
            .asList(new String[] { null, null, null, null });
    private List<@SafeHtml(level = SafeHtml.SafeListLevel.NONE) String> currentARVTreatmentINNs = Arrays
            .asList(new String[] { null, null, null, null });
    // private List<String> initialSampleConditionINNs= Arrays.asList(new String[]
    // {null, null, null, null});

    /** OI = opportunistic infection */
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String currentOITreatment;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String cotrimoxazoleTreatment;

    @Pattern(regexp = "^[0-9]*$")
    private String patientWeight;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String karnofskyScore;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String cd4Count;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String cd4Percent;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String initcd4Count;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String initcd4Percent;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String demandcd4Count;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String demandcd4Percent;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String priorCd4Date;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String antiTbTreatment;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String arvTreatmentAnyAdverseEffects;

    @Valid
    private List<AdverseEffect> arvTreatmentAdverseEffects = Arrays.asList(
            new AdverseEffect[] { new AdverseEffect(), new AdverseEffect(), new AdverseEffect(), new AdverseEffect() });

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String arvTreatmentChange;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String arvTreatmentNew;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String arvTreatmentRegime;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String cotrimoxazoleTreatmentAnyAdverseEffects;

    @Valid
    private List<AdverseEffect> cotrimoxazoleTreatmentAdverseEffects = Arrays.asList(
            new AdverseEffect[] { new AdverseEffect(), new AdverseEffect(), new AdverseEffect(), new AdverseEffect() });

    private String hospital;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String service;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String hospitalPatient;

    // Sample Forms
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String whichPCR;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String reasonForSecondPCRTest;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String indFirstTestName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String indSecondTestName;

    @ValidDate
    private String indFirstTestDate;
    @ValidDate
    private String indSecondTestDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String indFirstTestResult;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String indSecondTestResult;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String indSiteFinalResult;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String reasonForRequest;

    // VL Patient form
    @ValidDate
    private String arvTreatmentInitDate;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String vlReasonForRequest;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String vlOtherReasonForRequest;

    @ValidDate
    private String initcd4Date;
    @ValidDate
    private String demandcd4Date;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String vlBenefit;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String vlPregnancy;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String vlSuckle;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String priorVLLab;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ResultValidationForm.ResultValidation.class })
    private String priorVLValue;

    @ValidDate
    private String priorVLDate;

    // EID Patient form
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidInfantPTME;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidTypeOfClinic;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String eidTypeOfClinicOther;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidHowChildFed;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidStoppedBreastfeeding;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidInfantSymptomatic;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidMothersHIVStatus;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidMothersARV;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidInfantsARV;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String eidInfantCotrimoxazole;

    @ValidName(nameType = NameType.FULL_NAME)
    private String nameOfRequestor;

    @ValidName(nameType = NameType.FULL_NAME)
    private String nameOfSampler;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String hpvSamplingMethod;

    /** Yes/No */
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String anySecondaryTreatment;

    /** Actual treatment */
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String secondaryTreatment;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String clinicVisits;
    /** Reason for test submital */
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reason;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String underInvestigation;

    // diseases
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CTBPul;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CTBExpul;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CCrblToxo;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CCryptoMen;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CGenPrurigo;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CIST;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CCervCancer;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String COpharCand;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CKaposiSarc;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CShingles;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String CDiarrheaC;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PTBPul;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PTBExpul;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PCrblToxo;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PCryptoMen;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PGenPrurigo;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PIST;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PCervCancer;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String POpharCand;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PKaposiSarc;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PShingles;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String PDiarrheaC;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String weightLoss;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String diarrhea;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String fever;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String cough;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String pulTB;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String expulTB;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String swallPaint;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String cryptoMen;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String recPneumon;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String sespis;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String recInfect;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String curvixC;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String matHIV;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String cachexie;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String thrush;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String dermPruip;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String herpes;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String zona;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String sarcKapo;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String xIngPadenp;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String HIVDement;

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getNationalityOther() {
        return nationalityOther;
    }

    public void setNationalityOther(String nationalityOther) {
        this.nationalityOther = nationalityOther;
    }

    public String getLegalResidence() {
        return legalResidence;
    }

    public void setLegalResidence(String legalResidence) {
        this.legalResidence = legalResidence;
    }

    public String getNameOfDoctor() {
        return nameOfDoctor;
    }

    public void setNameOfDoctor(String nameOfDoctor) {
        this.nameOfDoctor = nameOfDoctor;
    }

    public void setAnyPriorDiseases(String anyPriorDiseases) {
        this.anyPriorDiseases = anyPriorDiseases;
    }

    public String getAnyPriorDiseases() {
        return anyPriorDiseases;
    }

    public void setPriorDiseases(String priorDiseases) {
        this.priorDiseases = priorDiseases;
    }

    public String getPriorDiseases() {
        return priorDiseases;
    }

    public String getArvProphylaxisBenefit() {
        return arvProphylaxisBenefit;
    }

    public void setArvProphylaxisBenefit(String arvProphylaxisBenefit) {
        this.arvProphylaxisBenefit = arvProphylaxisBenefit;
    }

    public String getArvProphylaxis() {
        return arvProphylaxis;
    }

    public void setArvProphylaxis(String arvProphylaxis) {
        this.arvProphylaxis = arvProphylaxis;
    }

    public void setCurrentARVTreatment(String currentARVTreatment) {
        this.currentARVTreatment = currentARVTreatment;
    }

    public String getCurrentARVTreatment() {
        return currentARVTreatment;
    }

    public void setPriorARVTreatment(String priorARVTreatment) {
        this.priorARVTreatment = priorARVTreatment;
    }

    public String getPriorARVTreatment() {
        return priorARVTreatment;
    }

    public void setCotrimoxazoleTreatment(String cotrimoxazoleTreatment) {
        this.cotrimoxazoleTreatment = cotrimoxazoleTreatment;
    }

    public String getCotrimoxazoleTreatment() {
        return cotrimoxazoleTreatment;
    }

    public void setAidsStage(String aidsStage) {
        this.aidsStage = aidsStage;
    }

    public String getAidsStage() {
        return aidsStage;
    }

    public void setAnyCurrentDiseases(String anyCurrentDiseases) {
        this.anyCurrentDiseases = anyCurrentDiseases;
    }

    public String getAnyCurrentDiseases() {
        return anyCurrentDiseases;
    }

    public void setCurrentDiseases(String currentDiseases) {
        this.currentDiseases = currentDiseases;
    }

    public String getCurrentDiseases() {
        return currentDiseases;
    }

    public void setCurrentOITreatment(String currentTreatment) {
        currentOITreatment = currentTreatment;
    }

    public String getCurrentOITreatment() {
        return currentOITreatment;
    }

    public String getPatientWeight() {
        return patientWeight;
    }

    public void setPatientWeight(String patientWeight) {
        this.patientWeight = patientWeight;
    }

    public String getKarnofskyScore() {
        return karnofskyScore;
    }

    public void setKarnofskyScore(String karnofskyScore) {
        this.karnofskyScore = karnofskyScore;
    }

    public String getHivStatus() {
        return hivStatus;
    }

    public void setHivStatus(String hivStatus) {
        this.hivStatus = hivStatus;
    }

    public String getCd4Count() {
        return cd4Count;
    }

    public void setCd4Count(String cd4Count) {
        this.cd4Count = cd4Count;
    }

    public String getInitcd4Count() {
        return initcd4Count;
    }

    public void setInitcd4Count(String initcd4Count) {
        this.initcd4Count = initcd4Count;
    }

    public String getDemandcd4Count() {
        return demandcd4Count;
    }

    public void setDemandcd4Count(String demandcd4Count) {
        this.demandcd4Count = demandcd4Count;
    }

    public String getInitcd4Date() {
        return initcd4Date;
    }

    public void setInitcd4Date(String initcd4Date) {
        this.initcd4Date = initcd4Date;
    }

    public String getDemandcd4Date() {
        return demandcd4Date;
    }

    public void setDemandcd4Date(String demandcd4Date) {
        this.demandcd4Date = demandcd4Date;
    }

    public String getCd4Percent() {
        return cd4Percent;
    }

    public void setCd4Percent(String cd4Percent) {
        this.cd4Percent = cd4Percent;
    }

    public String getInitcd4Percent() {
        return initcd4Percent;
    }

    public void setInitcd4Percent(String initcd4Percent) {
        this.initcd4Percent = initcd4Percent;
    }

    public String getDemandcd4Percent() {
        return demandcd4Percent;
    }

    public void setDemandcd4Percent(String demandcd4Percent) {
        this.demandcd4Percent = demandcd4Percent;
    }

    public String getPriorCd4Date() {
        return priorCd4Date;
    }

    public void setPriorCd4Date(String priorCd4Date) {
        this.priorCd4Date = priorCd4Date;
    }

    public String getAntiTbTreatment() {
        return antiTbTreatment;
    }

    public void setAntiTbTreatment(String antiTbTreatment) {
        this.antiTbTreatment = antiTbTreatment;
    }

    public void setInterruptedARVTreatment(String interruptedARV) {
        interruptedARVTreatment = interruptedARV;
    }

    public String getInterruptedARVTreatment() {
        return interruptedARVTreatment;
    }

    public String getPriorARVTreatmentINNs(int index) {
        return priorARVTreatmentINNs.get(index);
    }

    public void setPriorARVTreatmentINNs(int index, String value) {
        priorARVTreatmentINNs.set(index, value);
    }

    public List<String> getPriorARVTreatmentINNsList() {
        return priorARVTreatmentINNs;
    }

    public String getCurrentARVTreatmentINNs(int index) {
        return currentARVTreatmentINNs.get(index);
    }

    public void setCurrentARVTreatmentINNs(int index, String value) {
        currentARVTreatmentINNs.set(index, value);
    }

    public List<String> getCurrentARVTreatmentINNsList() {
        return currentARVTreatmentINNs;
    }

    /*
     * public String getInitialSampleConditionINNs(int index) { return
     * initialSampleConditionINNs.get(index); }
     *
     * public void setInitialSampleConditionINNs(int index, String value) {
     * initialSampleConditionINNs.set(index, value); }
     *
     * public List<String> getInitialSampleConditionINNsList() { return
     * initialSampleConditionINNs; }
     */

    public String getFutureARVTreatmentINNs(int index) {
        return futureARVTreatmentINNs.get(index);
    }

    public void setFutureARVTreatmentINNs(int index, String value) {
        futureARVTreatmentINNs.set(index, value);
    }

    public List<String> getFutureARVTreatmentINNsList() {
        return futureARVTreatmentINNs;
    }

    public void setArvTreatmentAnyAdverseEffects(String arvTreatmentAnyAdverseEffects) {
        this.arvTreatmentAnyAdverseEffects = arvTreatmentAnyAdverseEffects;
    }

    public String getArvTreatmentAnyAdverseEffects() {
        return arvTreatmentAnyAdverseEffects;
    }

    public List<AdverseEffect> getArvTreatmentAdverseEffects() {
        return arvTreatmentAdverseEffects;
    }

    public void setArvTreatmentChange(String arvTreatmentChange) {
        this.arvTreatmentChange = arvTreatmentChange;
    }

    public String getArvTreatmentChange() {
        return arvTreatmentChange;
    }

    public void setArvTreatmentNew(String arvTreatmentNew) {
        this.arvTreatmentNew = arvTreatmentNew;
    }

    public String getArvTreatmentNew() {
        return arvTreatmentNew;
    }

    public void setArvTreatmentRegime(String arvTreatmentRegime) {
        this.arvTreatmentRegime = arvTreatmentRegime;
    }

    public String getArvTreatmentRegime() {
        return arvTreatmentRegime;
    }

    public void setCotrimoxazoleTreatmentAnyAdverseEffects(String cotrimoxazoleTreatmentAnyAdverseEffects) {
        this.cotrimoxazoleTreatmentAnyAdverseEffects = cotrimoxazoleTreatmentAnyAdverseEffects;
    }

    public String getCotrimoxazoleTreatmentAnyAdverseEffects() {
        return cotrimoxazoleTreatmentAnyAdverseEffects;
    }

    public List<AdverseEffect> getCotrimoxazoleTreatmentAdverseEffects() {
        return cotrimoxazoleTreatmentAdverseEffects;
    }

    public void setAnySecondaryTreatment(String anySecondaryTreatment) {
        this.anySecondaryTreatment = anySecondaryTreatment;
    }

    public String getAnySecondaryTreatment() {
        return anySecondaryTreatment;
    }

    public void setSecondaryTreatment(String secondaryTreatment) {
        this.secondaryTreatment = secondaryTreatment;
    }

    public String getSecondaryTreatment() {
        return secondaryTreatment;
    }

    public void setClinicVisits(String clinicVisits) {
        this.clinicVisits = clinicVisits;
    }

    public String getClinicVisits() {
        return clinicVisits;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getHospital() {
        return hospital;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public void setHospitalPatient(String hospitalPatient) {
        this.hospitalPatient = hospitalPatient;
    }

    public String getHospitalPatient() {
        return hospitalPatient;
    }

    public String getWhichPCR() {
        return whichPCR;
    }

    public void setWhichPCR(String whichPCR) {
        this.whichPCR = whichPCR;
    }

    public String getReasonForSecondPCRTest() {
        return reasonForSecondPCRTest;
    }

    public void setReasonForSecondPCRTest(String reasonForSecondPCRTest) {
        this.reasonForSecondPCRTest = reasonForSecondPCRTest;
    }

    public String getIndFirstTestName() {
        return indFirstTestName;
    }

    public void setIndFirstTestName(String indFirstTestName) {
        this.indFirstTestName = indFirstTestName;
    }

    public String getIndSecondTestName() {
        return indSecondTestName;
    }

    public void setIndSecondTestName(String indSecondTestName) {
        this.indSecondTestName = indSecondTestName;
    }

    public String getIndFirstTestDate() {
        return indFirstTestDate;
    }

    public void setIndFirstTestDate(String indFirstTestDate) {
        this.indFirstTestDate = indFirstTestDate;
    }

    public String getIndSecondTestDate() {
        return indSecondTestDate;
    }

    public void setIndSecondTestDate(String indSecondTestDate) {
        this.indSecondTestDate = indSecondTestDate;
    }

    public String getIndFirstTestResult() {
        return indFirstTestResult;
    }

    public void setIndFirstTestResult(String indFirstTestResult) {
        this.indFirstTestResult = indFirstTestResult;
    }

    public String getIndSecondTestResult() {
        return indSecondTestResult;
    }

    public void setIndSecondTestResult(String indSecondTestResult) {
        this.indSecondTestResult = indSecondTestResult;
    }

    public String getIndSiteFinalResult() {
        return indSiteFinalResult;
    }

    public void setIndSiteFinalResult(String indSiteFinalResult) {
        this.indSiteFinalResult = indSiteFinalResult;
    }

    public void setReasonForRequest(String reasonForRequest) {
        this.reasonForRequest = reasonForRequest;
    }

    public String getReasonForRequest() {
        return reasonForRequest;
    }

    public String getEidInfantPTME() {
        return eidInfantPTME;
    }

    public void setEidInfantPTME(String eidInfantPTME) {
        this.eidInfantPTME = eidInfantPTME;
    }

    public String getEidTypeOfClinic() {
        return eidTypeOfClinic;
    }

    public void setEidTypeOfClinic(String eidTypeOfClinic) {
        this.eidTypeOfClinic = eidTypeOfClinic;
    }

    public String getEidHowChildFed() {
        return eidHowChildFed;
    }

    public void setEidHowChildFed(String eidHowChildFed) {
        this.eidHowChildFed = eidHowChildFed;
    }

    public String getEidInfantSymptomatic() {
        return eidInfantSymptomatic;
    }

    public void setEidInfantSymptomatic(String eidInfantSymptomatic) {
        this.eidInfantSymptomatic = eidInfantSymptomatic;
    }

    public String getEidMothersHIVStatus() {
        return eidMothersHIVStatus;
    }

    public void setEidMothersHIVStatus(String eidMothersHIVStatus) {
        this.eidMothersHIVStatus = eidMothersHIVStatus;
    }

    public String getEidMothersARV() {
        return eidMothersARV;
    }

    public void setEidMothersARV(String eidMothersARV) {
        this.eidMothersARV = eidMothersARV;
    }

    public String getEidInfantsARV() {
        return eidInfantsARV;
    }

    public void setEidInfantsARV(String eidInfantsARV) {
        this.eidInfantsARV = eidInfantsARV;
    }

    public String getEidInfantCotrimoxazole() {
        return eidInfantCotrimoxazole;
    }

    public void setEidInfantCotrimoxazole(String eidInfantCotrimoxazole) {
        this.eidInfantCotrimoxazole = eidInfantCotrimoxazole;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public String getProjectFormName() {
        return projectFormName;
    }

    public void setProjectFormName(String projectFormName) {
        this.projectFormName = projectFormName;
    }

    public String getNameOfRequestor() {
        return nameOfRequestor;
    }

    public void setNameOfRequestor(String nameOfRequestor) {
        this.nameOfRequestor = nameOfRequestor;
    }

    public String getNameOfSampler() {
        return nameOfSampler;
    }

    public void setNameOfSampler(String nameOfSampler) {
        this.nameOfSampler = nameOfSampler;
    }

    public void setUnderInvestigation(String underInvestigation) {
        this.underInvestigation = underInvestigation;
    }

    public String getUnderInvestigation() {
        return underInvestigation;
    }

    public void setEidTypeOfClinicOther(String eidTypeOfClinicOther) {
        this.eidTypeOfClinicOther = eidTypeOfClinicOther;
    }

    public String getEidTypeOfClinicOther() {
        return eidTypeOfClinicOther;
    }

    public void setEidStoppedBreastfeeding(String eidStoppedBreastfeeding) {
        this.eidStoppedBreastfeeding = eidStoppedBreastfeeding;
    }

    public String getEidStoppedBreastfeeding() {
        return eidStoppedBreastfeeding;
    }

    public List<String> getFutureARVTreatmentINNs() {
        return futureARVTreatmentINNs;
    }

    public void setFutureARVTreatmentINNs(List<String> futureARVTreatmentINNs) {
        this.futureARVTreatmentINNs = futureARVTreatmentINNs;
    }

    public String getCTBPul() {
        return CTBPul;
    }

    public void setCTBPul(String cTBPul) {
        CTBPul = cTBPul;
    }

    public String getCTBExpul() {
        return CTBExpul;
    }

    public void setCTBExpul(String cTBExpul) {
        CTBExpul = cTBExpul;
    }

    public String getCCrblToxo() {
        return CCrblToxo;
    }

    public void setCCrblToxo(String cCrblToxo) {
        CCrblToxo = cCrblToxo;
    }

    public String getCCryptoMen() {
        return CCryptoMen;
    }

    public void setCCryptoMen(String cCryptoMen) {
        CCryptoMen = cCryptoMen;
    }

    public String getCGenPrurigo() {
        return CGenPrurigo;
    }

    public void setCGenPrurigo(String cGenPrurigo) {
        CGenPrurigo = cGenPrurigo;
    }

    public String getCIST() {
        return CIST;
    }

    public void setCIST(String cIST) {
        CIST = cIST;
    }

    public String getCCervCancer() {
        return CCervCancer;
    }

    public void setCCervCancer(String cCervCancer) {
        CCervCancer = cCervCancer;
    }

    public String getCOpharCand() {
        return COpharCand;
    }

    public void setCOpharCand(String cOpharCand) {
        COpharCand = cOpharCand;
    }

    public String getCKaposiSarc() {
        return CKaposiSarc;
    }

    public void setCKaposiSarc(String cKaposiSarc) {
        CKaposiSarc = cKaposiSarc;
    }

    public String getCShingles() {
        return CShingles;
    }

    public void setCShingles(String cShingles) {
        CShingles = cShingles;
    }

    public String getCDiarrheaC() {
        return CDiarrheaC;
    }

    public void setCDiarrheaC(String cDiarrheaC) {
        CDiarrheaC = cDiarrheaC;
    }

    public String getPTBPul() {
        return PTBPul;
    }

    public void setPTBPul(String pTBPul) {
        PTBPul = pTBPul;
    }

    public String getPTBExpul() {
        return PTBExpul;
    }

    public void setPTBExpul(String pTBExpul) {
        PTBExpul = pTBExpul;
    }

    public String getPCrblToxo() {
        return PCrblToxo;
    }

    public void setPCrblToxo(String pCrblToxo) {
        PCrblToxo = pCrblToxo;
    }

    public String getPCryptoMen() {
        return PCryptoMen;
    }

    public void setPCryptoMen(String pCryptoMen) {
        PCryptoMen = pCryptoMen;
    }

    public String getPGenPrurigo() {
        return PGenPrurigo;
    }

    public void setPGenPrurigo(String pGenPrurigo) {
        PGenPrurigo = pGenPrurigo;
    }

    public String getPIST() {
        return PIST;
    }

    public void setPIST(String pIST) {
        PIST = pIST;
    }

    public String getPCervCancer() {
        return PCervCancer;
    }

    public void setPCervCancer(String pCervCancer) {
        PCervCancer = pCervCancer;
    }

    public String getPOpharCand() {
        return POpharCand;
    }

    public void setPOpharCand(String pOpharCand) {
        POpharCand = pOpharCand;
    }

    public String getPKaposiSarc() {
        return PKaposiSarc;
    }

    public void setPKaposiSarc(String pKaposiSarc) {
        PKaposiSarc = pKaposiSarc;
    }

    public String getPShingles() {
        return PShingles;
    }

    public void setPShingles(String pShingles) {
        PShingles = pShingles;
    }

    public String getPDiarrheaC() {
        return PDiarrheaC;
    }

    public void setPDiarrheaC(String pDiarrheaC) {
        PDiarrheaC = pDiarrheaC;
    }

    public String getWeightLoss() {
        return weightLoss;
    }

    public void setWeightLoss(String weightLoss) {
        this.weightLoss = weightLoss;
    }

    public String getDiarrhea() {
        return diarrhea;
    }

    public void setDiarrhea(String diarrhea) {
        this.diarrhea = diarrhea;
    }

    public String getFever() {
        return fever;
    }

    public void setFever(String fever) {
        this.fever = fever;
    }

    public String getCough() {
        return cough;
    }

    public void setCough(String cough) {
        this.cough = cough;
    }

    public String getPulTB() {
        return pulTB;
    }

    public void setPulTB(String pulTB) {
        this.pulTB = pulTB;
    }

    public String getExpulTB() {
        return expulTB;
    }

    public void setExpulTB(String expulTB) {
        this.expulTB = expulTB;
    }

    public String getSwallPaint() {
        return swallPaint;
    }

    public void setSwallPaint(String swallPaint) {
        this.swallPaint = swallPaint;
    }

    public String getCryptoMen() {
        return cryptoMen;
    }

    public void setCryptoMen(String cryptoMen) {
        this.cryptoMen = cryptoMen;
    }

    public String getRecPneumon() {
        return recPneumon;
    }

    public void setRecPneumon(String recPneumon) {
        this.recPneumon = recPneumon;
    }

    public String getSespis() {
        return sespis;
    }

    public void setSespis(String sespis) {
        this.sespis = sespis;
    }

    public String getRecInfect() {
        return recInfect;
    }

    public void setRecInfect(String recInfect) {
        this.recInfect = recInfect;
    }

    public String getCurvixC() {
        return curvixC;
    }

    public void setCurvixC(String curvixC) {
        this.curvixC = curvixC;
    }

    public String getMatHIV() {
        return matHIV;
    }

    public void setMatHIV(String matHIV) {
        this.matHIV = matHIV;
    }

    public String getCachexie() {
        return cachexie;
    }

    public void setCachexie(String cachexie) {
        this.cachexie = cachexie;
    }

    public String getThrush() {
        return thrush;
    }

    public void setThrush(String thrush) {
        this.thrush = thrush;
    }

    public String getDermPruip() {
        return dermPruip;
    }

    public void setDermPruip(String dermPruip) {
        this.dermPruip = dermPruip;
    }

    public String getHerpes() {
        return herpes;
    }

    public void setHerpes(String herpes) {
        this.herpes = herpes;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getSarcKapo() {
        return sarcKapo;
    }

    public void setSarcKapo(String sarcKapo) {
        this.sarcKapo = sarcKapo;
    }

    public String getxIngPadenp() {
        return xIngPadenp;
    }

    public void setxIngPadenp(String xIngPadenp) {
        this.xIngPadenp = xIngPadenp;
    }

    public String getHIVDement() {
        return HIVDement;
    }

    public void setHIVDement(String hIVDement) {
        HIVDement = hIVDement;
    }

    public String getPriorDiseasesValue() {
        return priorDiseasesValue;
    }

    public void setPriorDiseasesValue(String priorDiseasesValue) {
        this.priorDiseasesValue = priorDiseasesValue;
    }

    public String getCurrentDiseasesValue() {
        return currentDiseasesValue;
    }

    public void setCurrentDiseasesValue(String currentDiseasesValue) {
        this.currentDiseasesValue = currentDiseasesValue;
    }

    public String getArvTreatmentInitDate() {
        return arvTreatmentInitDate;
    }

    public void setArvTreatmentInitDate(String arvTreatmentInitDate) {
        this.arvTreatmentInitDate = arvTreatmentInitDate;
    }

    public String getVlReasonForRequest() {
        return vlReasonForRequest;
    }

    public void setVlReasonForRequest(String vlReasonForRequest) {
        this.vlReasonForRequest = vlReasonForRequest;
    }

    public String getVlOtherReasonForRequest() {
        return vlOtherReasonForRequest;
    }

    public void setVlOtherReasonForRequest(String vlOtherReasonForRequest) {
        this.vlOtherReasonForRequest = vlOtherReasonForRequest;
    }

    public String getVlBenefit() {
        return vlBenefit;
    }

    public void setVlBenefit(String vlBenefit) {
        this.vlBenefit = vlBenefit;
    }

    // -----------
    public String getVlPregnancy() {
        return vlPregnancy;
    }

    public void setVlPregnancy(String vlPregnancy) {
        this.vlPregnancy = vlPregnancy;
    }

    public String getVlSuckle() {
        return vlSuckle;
    }

    public void setVlSuckle(String vlSuckle) {
        this.vlSuckle = vlSuckle;
    }
    // -------------

    public String getPriorVLLab() {
        return priorVLLab;
    }

    public void setPriorVLLab(String priorVLLab) {
        this.priorVLLab = priorVLLab;
    }

    public String getPriorVLValue() {
        return priorVLValue;
    }

    public void setPriorVLValue(String priorVLValue) {
        this.priorVLValue = priorVLValue;
    }

    public String getPriorVLDate() {
        return priorVLDate;
    }

    public void setPriorVLDate(String priorVLDate) {
        this.priorVLDate = priorVLDate;
    }

    public List<Pair<String, String>> getPriorDiseasesList() {
        if (priorDiseasesList == null) {
            priorDiseasesList = makeDiseaseList("P", ObservationHistoryList.ARV_DISEASES.getList());
        }
        return new ArrayList<>(new HashSet<>(priorDiseasesList));
    }

    private List<Pair<String, String>> makeDiseaseList(String prefix, List<Dictionary> dictionaryList) {
        List<Pair<String, String>> nvList = new ArrayList<>();
        for (Dictionary dictionary : dictionaryList) {
            nvList.add(Pair.of(prefix + dictionary.getLocalAbbreviation(), dictionary.getLocalizedName()));
        }
        return nvList;
    }

    public Pair<String, String> getPriorDiseases(int index) {
        return getPriorDiseasesList().get(index);
    }

    public List<Pair<String, String>> getCurrentDiseasesList() {
        if (currentDiseasesList == null) {
            currentDiseasesList = makeDiseaseList("C", ObservationHistoryList.ARV_DISEASES_SHORT.getList());
        }
        return new ArrayList<>(new HashSet<>(currentDiseasesList)); // remove duplicates entry
    }

    public Pair<String, String> getCurrentDiseases(int index) {
        return getCurrentDiseasesList().get(index);
    }

    public List<Pair<String, String>> getRtnPriorDiseasesList() {
        if (rtnPriorDiseasesList == null) {
            rtnPriorDiseasesList = makeDiseaseList("", ObservationHistoryList.RTN_DISEASES.getList());
        }
        return new ArrayList<>(new HashSet<>(rtnPriorDiseasesList));
    }

    public List<Pair<String, String>> getRtnCurrentDiseasesList() {
        if (rtnCurrentDiseasesList == null) {
            rtnCurrentDiseasesList = makeDiseaseList("", ObservationHistoryList.RTN_EXAM_DISEASES.getList());
        }
        return new ArrayList<>(new HashSet<>(rtnCurrentDiseasesList));
    }

    public String getHpvSamplingMethod() {
        return hpvSamplingMethod;
    }

    public void setHpvSamplingMethod(String hpvSamplingMethod) {
        this.hpvSamplingMethod = hpvSamplingMethod;
    }
}
