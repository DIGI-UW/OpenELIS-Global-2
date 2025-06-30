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
package org.openelisglobal.patient.action.bean;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.sample.form.SamplePatientEntryForm.SamplePatientEntryBatch;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntryForm;
import org.openelisglobal.validation.annotations.SafeHtml;

public class PatientClinicalInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String YES_NO_UNKNOWN_REGEX = "^yes$|^no|^unknown$";

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String tbExtraPulmanary;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String tbCerebral;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String tbMenigitis;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String tbPrurigol;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String tbDiarrhae;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdColonCancer;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdCandidiasis;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdKaposi;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdZona;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdOther;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvProphyaxixReceiving;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvProphyaxixType;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvTreatmentReceiving;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvTreatmentRemembered;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvTreatment1;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvTreatment2;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvTreatment3;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String arvTreatment4;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String cotrimoxazoleReceiving;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String cotrimoxazoleType;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String infectionExtraPulmanary;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String infectionCerebral;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String infectionMeningitis;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String infectionPrurigol;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String infectionOther;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdInfectionColon;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdInfectionCandidiasis;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdInfectionKaposi;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String stdInfectionZona;

    @Pattern(regexp = YES_NO_UNKNOWN_REGEX, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String infectionUnderTreatment;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String weight;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryBatch.class })
    private String karnofskyScore;

    // for display
    private List<IdValuePair> prophyaxixTypes;

    // for display
    private List<IdValuePair> arvStages;

    public String getTbExtraPulmanary() {
        return tbExtraPulmanary;
    }

    public void setTbExtraPulmanary(String tbExtraPulmanary) {
        this.tbExtraPulmanary = tbExtraPulmanary;
    }

    public String getTbCerebral() {
        return tbCerebral;
    }

    public void setTbCerebral(String tbCerebral) {
        this.tbCerebral = tbCerebral;
    }

    public String getTbMenigitis() {
        return tbMenigitis;
    }

    public void setTbMenigitis(String tbMenigitis) {
        this.tbMenigitis = tbMenigitis;
    }

    public String getTbPrurigol() {
        return tbPrurigol;
    }

    public void setTbPrurigol(String tbPrurigol) {
        this.tbPrurigol = tbPrurigol;
    }

    public String getTbDiarrhae() {
        return tbDiarrhae;
    }

    public void setTbDiarrhae(String tbDiarrhae) {
        this.tbDiarrhae = tbDiarrhae;
    }

    public String getStdColonCancer() {
        return stdColonCancer;
    }

    public void setStdColonCancer(String stdColonCancer) {
        this.stdColonCancer = stdColonCancer;
    }

    public String getStdCandidiasis() {
        return stdCandidiasis;
    }

    public void setStdCandidiasis(String stdCandidiasis) {
        this.stdCandidiasis = stdCandidiasis;
    }

    public String getStdKaposi() {
        return stdKaposi;
    }

    public void setStdKaposi(String stdKaposi) {
        this.stdKaposi = stdKaposi;
    }

    public String getStdZona() {
        return stdZona;
    }

    public void setStdZona(String stdZona) {
        this.stdZona = stdZona;
    }

    public String getStdOther() {
        return stdOther;
    }

    public void setStdOther(String stdOther) {
        this.stdOther = stdOther;
    }

    public String getArvProphyaxixReceiving() {
        return arvProphyaxixReceiving;
    }

    public void setArvProphyaxixReceiving(String arvProphyaxixReceiving) {
        this.arvProphyaxixReceiving = arvProphyaxixReceiving;
    }

    public String getArvProphyaxixType() {
        return arvProphyaxixType;
    }

    public void setArvProphyaxixType(String arvProphyaxixType) {
        this.arvProphyaxixType = arvProphyaxixType;
    }

    public String getArvTreatmentReceiving() {
        return arvTreatmentReceiving;
    }

    public void setArvTreatmentReceiving(String arvTreatmentReceiving) {
        this.arvTreatmentReceiving = arvTreatmentReceiving;
    }

    public String getArvTreatmentRemembered() {
        return arvTreatmentRemembered;
    }

    public void setArvTreatmentRemembered(String arvTreatmentRemembered) {
        this.arvTreatmentRemembered = arvTreatmentRemembered;
    }

    public String getArvTreatment1() {
        return arvTreatment1;
    }

    public void setArvTreatment1(String arvTreatment1) {
        this.arvTreatment1 = arvTreatment1;
    }

    public String getArvTreatment2() {
        return arvTreatment2;
    }

    public void setArvTreatment2(String arvTreatment2) {
        this.arvTreatment2 = arvTreatment2;
    }

    public String getArvTreatment3() {
        return arvTreatment3;
    }

    public void setArvTreatment3(String arvTreatment3) {
        this.arvTreatment3 = arvTreatment3;
    }

    public String getArvTreatment4() {
        return arvTreatment4;
    }

    public void setArvTreatment4(String arvTreatment4) {
        this.arvTreatment4 = arvTreatment4;
    }

    public String getCotrimoxazoleReceiving() {
        return cotrimoxazoleReceiving;
    }

    public void setCotrimoxazoleReceiving(String cotrimoxazoleReceiving) {
        this.cotrimoxazoleReceiving = cotrimoxazoleReceiving;
    }

    public String getCotrimoxazoleType() {
        return cotrimoxazoleType;
    }

    public void setCotrimoxazoleType(String cotrimoxazoleType) {
        this.cotrimoxazoleType = cotrimoxazoleType;
    }

    public String getInfectionExtraPulmanary() {
        return infectionExtraPulmanary;
    }

    public void setInfectionExtraPulmanary(String infectionExtraPulmanary) {
        this.infectionExtraPulmanary = infectionExtraPulmanary;
    }

    public String getInfectionMeningitis() {
        return infectionMeningitis;
    }

    public void setInfectionMeningitis(String infectionMenigitis) {
        infectionMeningitis = infectionMenigitis;
    }

    public String getInfectionPrurigol() {
        return infectionPrurigol;
    }

    public void setInfectionPrurigol(String infectionPrurigol) {
        this.infectionPrurigol = infectionPrurigol;
    }

    public String getInfectionOther() {
        return infectionOther;
    }

    public void setInfectionOther(String infectionOther) {
        this.infectionOther = infectionOther;
    }

    public String getStdInfectionColon() {
        return stdInfectionColon;
    }

    public void setStdInfectionColon(String stdInfectionColon) {
        this.stdInfectionColon = stdInfectionColon;
    }

    public String getStdInfectionCandidiasis() {
        return stdInfectionCandidiasis;
    }

    public void setStdInfectionCandidiasis(String stdInfectionCandidiasis) {
        this.stdInfectionCandidiasis = stdInfectionCandidiasis;
    }

    public String getStdInfectionKaposi() {
        return stdInfectionKaposi;
    }

    public void setStdInfectionKaposi(String stdInfectionKaposi) {
        this.stdInfectionKaposi = stdInfectionKaposi;
    }

    public String getStdInfectionZona() {
        return stdInfectionZona;
    }

    public void setStdInfectionZona(String stdInfectionZona) {
        this.stdInfectionZona = stdInfectionZona;
    }

    public String getInfectionUnderTreatment() {
        return infectionUnderTreatment;
    }

    public void setInfectionUnderTreatment(String infectionUnderTreatment) {
        this.infectionUnderTreatment = infectionUnderTreatment;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getKarnofskyScore() {
        return karnofskyScore;
    }

    public void setKarnofskyScore(String karnofskyScore) {
        this.karnofskyScore = karnofskyScore;
    }

    public List<IdValuePair> getProphyaxixTypes() {
        if (prophyaxixTypes == null) {
            prophyaxixTypes = new ArrayList<>();
        }
        return prophyaxixTypes;
    }

    public void setProphyaxixTypes(List<IdValuePair> prophyaxixTypes) {
        this.prophyaxixTypes = prophyaxixTypes;
    }

    public List<IdValuePair> getArvStages() {
        if (arvStages == null) {
            arvStages = new ArrayList<>();
        }
        return arvStages;
    }

    public void setArvStages(List<IdValuePair> arvStages) {
        this.arvStages = arvStages;
    }

    public String getInfectionCerebral() {
        return infectionCerebral;
    }

    public void setInfectionCerebral(String infectionCerebral) {
        this.infectionCerebral = infectionCerebral;
    }
}
