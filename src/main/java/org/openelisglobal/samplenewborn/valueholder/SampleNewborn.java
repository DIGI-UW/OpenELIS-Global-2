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
package org.openelisglobal.samplenewborn.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;

public class SampleNewborn extends BaseObject<String> {

    private String id;
    private String weight;
    private String multiBirth;
    private String birthOrder;
    private double gestationalWeek;
    private Timestamp dateFirstFeeding;
    private String dateFirstFeedingForDisplay;
    private String breast;
    private String tpn;
    private String formula;
    private String milk;
    private String soy;
    private String jaundice;
    private String antibiotic;
    private String transfused;
    private Timestamp dateTransfution;
    private String dateTransfutionForDisplay;
    private String medicalRecordNumber;
    private String nicu;
    private String birthDefect;
    private String pregnancyComplication;
    private String deceasedSibling;
    private String causeOfDeath;
    private String familyHistory;
    private String other;
    private String yNumber;
    private String yellowCard;

    public SampleNewborn() {
        super();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getMultiBirth() {
        return multiBirth;
    }

    public void setMultiBirth(String multiBirth) {
        this.multiBirth = multiBirth;
    }

    public String getBirthOrder() {
        return birthOrder;
    }

    public void setBirthOrder(String birthOrder) {
        this.birthOrder = birthOrder;
    }

    public double getGestationalWeek() {
        return gestationalWeek;
    }

    public void setGestationalWeek(double gestationalWeek) {
        this.gestationalWeek = gestationalWeek;
    }

    public Timestamp getDateFirstFeeding() {
        return dateFirstFeeding;
    }

    public void setDateFirstFeeding(Timestamp dateFirstFeeding) {
        this.dateFirstFeeding = dateFirstFeeding;
        this.dateFirstFeedingForDisplay = DateUtil.convertTimestampToStringDate(dateFirstFeeding);
    }

    public String getDateFirstFeedingForDisplay() {
        return this.dateFirstFeedingForDisplay;
    }

    public void setDateFirstFeedingForDisplay(String dateFirstFeedingForDisplay) {
        this.dateFirstFeedingForDisplay = dateFirstFeedingForDisplay;
    }

    public void setBreast(String breast) {
        this.breast = breast;
    }

    public String getBreast() {
        return breast;
    }

    public void setTpn(String tpn) {
        this.tpn = tpn;
    }

    public String getTpn() {
        return tpn;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getFormula() {
        return formula;
    }

    public void setMilk(String milk) {
        this.milk = milk;
    }

    public String getMilk() {
        return milk;
    }

    public void setSoy(String soy) {
        this.soy = soy;
    }

    public String getSoy() {
        return soy;
    }

    public void setJaundice(String jaundice) {
        this.jaundice = jaundice;
    }

    public String getJaundice() {
        return jaundice;
    }

    public void setAntibiotic(String antibiotic) {
        this.antibiotic = antibiotic;
    }

    public String getAntibiotic() {
        return antibiotic;
    }

    public void setTransfused(String transfused) {
        this.transfused = transfused;
    }

    public String getTransfused() {
        return transfused;
    }

    public Timestamp getDateTransfution() {
        return dateTransfution;
    }

    public void setDateTransfution(Timestamp dateTransfution) {
        this.dateTransfution = dateTransfution;
        this.dateTransfutionForDisplay = DateUtil.convertTimestampToStringDate(dateTransfution);
    }

    public String getDateTransfutionForDisplay() {
        return this.dateTransfutionForDisplay;
    }

    public void setDateTransfutionForDisplay(String dateTransfutionForDisplay) {
        this.dateTransfutionForDisplay = dateTransfutionForDisplay;
    }

    public String getMedicalRecordNumber() {
        return medicalRecordNumber;
    }

    public void setMedicalRecordNumber(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }

    public String getNicu() {
        return nicu;
    }

    public void setNicu(String nicu) {
        this.nicu = nicu;
    }

    public String getBirthDefect() {
        return birthDefect;
    }

    public void setBirthDefect(String birthDefect) {
        this.birthDefect = birthDefect;
    }

    public String getPregnancyComplication() {
        return pregnancyComplication;
    }

    public void setPregnancyComplication(String pregnancyComplication) {
        this.pregnancyComplication = pregnancyComplication;
    }

    public String getDeceasedSibling() {
        return deceasedSibling;
    }

    public void setDeceasedSibling(String deceasedSibling) {
        this.deceasedSibling = deceasedSibling;
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }

    public void setCauseOfDeath(String causeOfDeath) {
        this.causeOfDeath = causeOfDeath;
    }

    public String getFamilyHistory() {
        return familyHistory;
    }

    public void setFamilyHistory(String familyHistory) {
        this.familyHistory = familyHistory;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public String getYnumber() {
        return yNumber;
    }

    public void setYnumber(String yNumber) {
        this.yNumber = yNumber;
    }

    public String getYellowCard() {
        return yellowCard;
    }

    public void setYellowCard(String yellowCard) {
        this.yellowCard = yellowCard;
    }
}
