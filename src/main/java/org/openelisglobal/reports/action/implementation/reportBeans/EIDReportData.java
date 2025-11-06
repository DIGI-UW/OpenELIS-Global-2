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
package org.openelisglobal.reports.action.implementation.reportBeans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public class EIDReportData {

    SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);

    private String hiv_status = "X";
    private String pcr_type = "X";
    private String subjectno;
    private String sitesubjectno;
    private String birth_date;
    private String ageMonth = "--";
    private String ageWeek = "--";
    private String gender;
    private String collectiondate;
    private String receptiondate;
    private String accession_number;
    private String servicename;
    private String doctor;
    private String compleationdate;
    private String subjectNumber;
    private String PTME;
    private String clinicDistrict;
    private String clinic;
    private String status;
    private Boolean duplicateReport = Boolean.FALSE;

    private List<SampleQaEvent> sampleQAEventList;
    List<QaEventItem> qaEventItems;

    private String allQaEvents = null;
    private String receptionQaEvent = null;
    private String virologyEidQaEvent = null;

    private Map<String, String> previousResultMap = new HashMap<>();

    public String getHiv_status() {
        return hiv_status;
    }

    public void setHiv_status(String hivStatus) {
        hiv_status = hivStatus;
    }

    public String getPcr_type() {
        return pcr_type;
    }

    public void setPcr_type(String pcrType) {
        pcr_type = pcrType;
    }

    public String getSubjectno() {
        return subjectno;
    }

    public void setSubjectno(String subjectno) {
        this.subjectno = subjectno;
    }

    public String getSitesubjectno() {
        return sitesubjectno;
    }

    public void setSitesubjectno(String sitesubjectno) {
        this.sitesubjectno = sitesubjectno;
    }

    public String getBirth_date() {
        return birth_date;
    }

    public void setBirth_date(String birthDate) {
        birth_date = birthDate;
    }

    public String getAgeMonth() {
        return ageMonth;
    }

    public void setAgeMonth(String agemonth) {
        ageMonth = agemonth;
    }

    public String getAgeWeek() {
        return ageWeek;
    }

    public void setAgeWeek(String ageWeek) {
        this.ageWeek = ageWeek;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCollectiondate() {
        return collectiondate;
    }

    public void setCollectiondate(String collectiondate) {
        this.collectiondate = collectiondate;
    }

    public String getAccession_number() {
        return accession_number;
    }

    public void setAccessionNumber(String accessionNumber) {
        accession_number = accessionNumber;
    }

    public String getServicename() {
        return servicename;
    }

    public void setServicename(String servicename) {
        this.servicename = servicename;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    public String getCompleationdate() {
        return compleationdate;
    }

    public void setCompleationdate(String compleationdate) {
        this.compleationdate = compleationdate;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public String getPTME() {
        return PTME;
    }

    public void setPTME(String pTME) {
        PTME = pTME;
    }

    public String getClinicDistrict() {
        return clinicDistrict;
    }

    public void setClinicDistrict(String clinicDistrict) {
        this.clinicDistrict = clinicDistrict;
    }

    public String getClinic() {
        return clinic;
    }

    public void setClinic(String clinic) {
        this.clinic = clinic;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getReceptiondate() {
        return receptiondate;
    }

    public void setReceptiondate(String receptiondate) {
        this.receptiondate = receptiondate;
    }

    public Boolean getDuplicateReport() {
        return duplicateReport;
    }

    public void setDuplicateReport(Boolean duplicateReport) {
        this.duplicateReport = duplicateReport;
    }

    public String getVirologyEidQaEvent() {
        return virologyEidQaEvent;
    }

    public void setVirologyEidQaEvent(String virologyEidQaEvent) {
        this.virologyEidQaEvent = virologyEidQaEvent;
    }

    public String getAllQaEvents() {
        return allQaEvents;
    }

    public void setAllQaEvents(String allQaEvents) {
        this.allQaEvents = allQaEvents;
    }

    public String getReceptionQaEvent() {
        return receptionQaEvent;
    }

    public void setReceptionQaEvent(String receptionQaEvent) {
        this.receptionQaEvent = receptionQaEvent;
    }

    public Map<String, String> getPreviousResultMap() {
        return previousResultMap;
    }

    public void setPreviousResultMap(Map<String, String> previousResultMap) {
        this.previousResultMap = previousResultMap;
    }

    public void getSampleQaEventItems(Sample sample) {
        qaEventItems = new ArrayList<>();
        if (sample != null) {
            getSampleQaEvents(sample);
            for (SampleQaEvent event : sampleQAEventList) {
                QAService qa = new QAService(event);
                QaEventItem item = new QaEventItem();
                item.setId(qa.getEventId());
                item.setQaEvent(qa.getQAEvent().getId());
                SampleItem sampleItem = qa.getSampleItem();
                // -1 is the index for "all samples"
                String sampleType = (sampleItem == null) ? "-1" : sampleItem.getTypeOfSample().getNameKey();
                allQaEvents = allQaEvents == null ? sampleType + ":" + qa.getQAEvent().getNameKey()
                        : allQaEvents + ";" + sampleType + ":" + qa.getQAEvent().getNameKey();

                if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.EID")) {
                    virologyEidQaEvent = virologyEidQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : virologyEidQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                }
            }
        }
    }

    public void getSampleQaEvents(Sample sample) {
        sampleQAEventList = sampleQaEventService.getSampleQaEventsBySample(sample);
    }
}
