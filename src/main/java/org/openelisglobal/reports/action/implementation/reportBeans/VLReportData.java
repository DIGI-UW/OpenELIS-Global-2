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
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public class VLReportData {

    private String ampli2;
    private String ampli2lo;
    private String subjectno;
    private String sitesubjectno;
    private String birth_date;
    private String age;
    private String ageMonth = "--";
    private String ageWeek = "--";
    private String gender;
    private String collectiondate;
    private String receptiondate;
    private String accession_number;
    private String servicename;
    private String doctor;
    private String compleationdate;
    private String releasedate;
    private String vlPregnancy;
    private String vlSuckle;
    private String PTME;
    private String clinicDistrict;
    private String clinic;
    private String status;
    private String vih;
    private String sampleTypeName;
    private Boolean duplicateReport = Boolean.FALSE;

    private List<SampleQaEvent> sampleQAEventList;
    private String allQaEvents = null;
    List<QaEventItem> qaEventItems;
    private String sampleQAEventNotes = "";
    private String virologyVlQaEvent = null;
    private String receptionQaEvent = null;

    private Map<String, String> previousResultMap = new HashMap<>();

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
        this.birth_date = birthDate;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAgeMonth() {
        return ageMonth;
    }

    public void setAgeMonth(String agemonth) {
        this.ageMonth = agemonth;
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

    public String getAmpli2() {
        return ampli2;
    }

    public void setAmpli2(String ampli2) {
        this.ampli2 = ampli2;
    }

    public String getAmpli2lo() {
        return ampli2lo;
    }

    public void setAmpli2lo(String ampli2lo) {
        this.ampli2lo = ampli2lo;
    }

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

    public String getvih() {
        return vih;
    }

    public void setvih(String vih) {
        this.vih = vih;
    }

    public String getSampleTypeName() {
        return sampleTypeName;
    }

    public void setSampleTypeName(String sampleTypeName) {
        this.sampleTypeName = sampleTypeName;
    }

    public Boolean getDuplicateReport() {
        return duplicateReport;
    }

    public void setDuplicateReport(Boolean duplicateReport) {
        this.duplicateReport = duplicateReport;
    }

    public String getVirologyVlQaEvent() {
        return virologyVlQaEvent;
    }

    public void setVirologyVlQaEvent(String virologyVlQaEvent) {
        this.virologyVlQaEvent = virologyVlQaEvent;
    }

    public String getAllQaEvents() {
        return allQaEvents;
    }

    public void setAllQaEvents(String allQaEvents) {
        this.allQaEvents = allQaEvents;
    }

    public String getSampleQAEventNotes() {
        return sampleQAEventNotes;
    }

    public void setSampleQAEventNotes(String sampleQAEventNotes) {
        this.sampleQAEventNotes = sampleQAEventNotes;
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
                // String sampleType=(sampleItem == null) ? "-1" :
                // sampleItem.getTypeOfSample().getNameKey();
                // allQaEvents=allQaEvents==null?sampleType+":"+qa.getQAEvent().getNameKey():allQaEvents+";"+sampleType+":"+qa.getQAEvent().getNameKey();

                if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.VL")) {
                    // virologyVlQaEvent=virologyVlQaEvent==null ?
                    // qa.getQAEvent().getLocalizedName() : virologyVlQaEvent+" ,
                    // "+qa.getQAEvent().getLocalizedName();
                    sampleQAEventNotes = sampleQAEventNotes + "  " + getNoteForSampleQaEvent(event);
                    String sampleType = (sampleItem == null) ? "-1" : sampleItem.getTypeOfSample().getNameKey();
                    allQaEvents = allQaEvents == null ? sampleType + ":" + qa.getQAEvent().getNameKey()
                            : allQaEvents + ";" + sampleType + ":" + qa.getQAEvent().getNameKey();
                }
            }
        }
    }

    public void getSampleQaEvents(Sample sample) {
        SampleService sampleService = SpringContext.getBean(SampleService.class);
        sampleQAEventList = sampleService.getSampleQAEventList(sample);
    }

    public static String getNoteForSampleQaEvent(SampleQaEvent sampleQaEvent) {
        if (sampleQaEvent == null || GenericValidator.isBlankOrNull(sampleQaEvent.getId())) {
            return null;
        } else {
            NoteService noteService = SpringContext.getBean(NoteService.class);
            Note note = noteService.getMostRecentNoteFilteredBySubject(sampleQaEvent, null);
            return note != null ? note.getText() : null;
        }
    }

    public String getReleasedate() {
        return releasedate;
    }

    public void setReleasedate(String releasedate) {
        this.releasedate = releasedate;
    }
}
