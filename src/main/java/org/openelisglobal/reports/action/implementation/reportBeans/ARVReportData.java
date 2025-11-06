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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public class ARVReportData {

    private String glyc;
    private String creatininemie;
    private String sgpt;
    private String sgot;
    private String gb;
    private String gr;
    private String hb;
    private String hct;
    private String vgm;
    private String plq;
    private String tcmh;
    private String ccmh;
    private String cd4per;
    private String nper;
    private String lper;
    private String mper;
    private String eoper;
    private String bper;
    private String cd4;
    private String vih;
    private String ampli2;
    private String ampli2lo;
    private String subjectNumber;
    private String birth_date;
    private String age;
    private String gender;
    private String collectiondate;
    private String receptiondate;
    private String orgname;
    private String doctor;
    private String compleationdate;
    private String releasedate;
    private String labNo;
    private String pcr;
    private String status;
    private Boolean showVirologie = Boolean.FALSE;
    private Boolean showSerologie = Boolean.FALSE;
    private Boolean showPCR = Boolean.FALSE;
    private Boolean duplicateReport = Boolean.FALSE;

    private List<SampleQaEvent> sampleQAEventList;
    private String allQaEvents = null;
    private String receptionQaEvent = null;
    private String biochemistryQaEvent = null;
    private String virologyQaEvent = null;
    private String virologyEidQaEvent = null;
    private String virologyVlQaEvent = null;
    private String serologyQaEvent = null;
    private String immunologyQaEvent = null;
    private String hematologyQaEvent = null;

    private Map<String, String> previousResultMap = new HashMap<>();

    SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);

    public String getGlyc() {
        return glyc;
    }

    public void setGlyc(String glyc) {
        this.glyc = glyc;
    }

    public String getCreatininemie() {
        return creatininemie;
    }

    public void setCreatininemie(String creatininemie) {
        this.creatininemie = creatininemie;
    }

    public String getSgpt() {
        return sgpt;
    }

    public void setSgpt(String sgpt) {
        this.sgpt = sgpt;
    }

    public String getSgot() {
        return sgot;
    }

    public void setSgot(String sgot) {
        this.sgot = sgot;
    }

    public String getGb() {
        return gb;
    }

    public void setGb(String gb) {
        this.gb = gb;
    }

    public String getGr() {
        return gr;
    }

    public void setGr(String gr) {
        this.gr = gr;
    }

    public String getHb() {
        return hb;
    }

    public void setHb(String hb) {
        this.hb = hb;
    }

    public String getHct() {
        return hct;
    }

    public void setHct(String hct) {
        this.hct = hct;
    }

    public String getVgm() {
        return vgm;
    }

    public void setVgm(String vgm) {
        this.vgm = vgm;
    }

    public String getPlq() {
        return plq;
    }

    public void setPlq(String plq) {
        this.plq = plq;
    }

    public String getCd4per() {
        return cd4per;
    }

    public void setCd4per(String cd4per) {
        this.cd4per = cd4per;
    }

    public String getNper() {
        return nper;
    }

    public void setNper(String nper) {
        this.nper = nper;
    }

    public String getLper() {
        return lper;
    }

    public void setLper(String lper) {
        this.lper = lper;
    }

    public String getMper() {
        return mper;
    }

    public void setMper(String mper) {
        this.mper = mper;
    }

    public String getEoper() {
        return eoper;
    }

    public void setEoper(String eoper) {
        this.eoper = eoper;
    }

    public String getBper() {
        return bper;
    }

    public void setBper(String bper) {
        this.bper = bper;
    }

    public String getCd4() {
        return cd4;
    }

    public void setCd4(String cd4) {
        this.cd4 = cd4;
    }

    public String getVih() {
        return vih;
    }

    public void setVih(String vih) {
        this.vih = vih;
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

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getBirth_date() {
        return birth_date;
    }

    public void setBirth_date(String birthDate) {
        birth_date = birthDate;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
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

    public String getOrgname() {
        return orgname;
    }

    public void setOrgname(String orgname) {
        this.orgname = orgname;
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

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getTcmh() {
        return tcmh;
    }

    public void setTcmh(String tcmh) {
        this.tcmh = tcmh;
    }

    public String getCcmh() {
        return ccmh;
    }

    public void setCcmh(String ccmh) {
        this.ccmh = ccmh;
    }

    public String getPcr() {
        return pcr;
    }

    public void setPcr(String pcr) {
        this.pcr = pcr;
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

    public Boolean getShowVirologie() {
        return showVirologie;
    }

    public void setShowVirologie(Boolean showVirologie) {
        this.showVirologie = showVirologie;
    }

    public Boolean getShowSerologie() {
        return showSerologie;
    }

    public void setShowSerologie(Boolean showSerologie) {
        this.showSerologie = showSerologie;
    }

    public Boolean getShowPCR() {
        return showPCR;
    }

    public void setShowPCR(Boolean showPCR) {
        this.showPCR = showPCR;
    }

    public Boolean getDuplicateReport() {
        return duplicateReport;
    }

    public void setDuplicateReport(Boolean duplicateReport) {
        this.duplicateReport = duplicateReport;
    }

    public String getReceptionQaEvent() {
        return receptionQaEvent;
    }

    public void setReceptionQaEvent(String receptionQaEvent) {
        this.receptionQaEvent = receptionQaEvent;
    }

    public String getBiochemistryQaEvent() {
        return biochemistryQaEvent;
    }

    public void setBiochemistryQaEvent(String biochemistryQaEvent) {
        this.biochemistryQaEvent = biochemistryQaEvent;
    }

    public String getVirologyQaEvent() {
        return virologyQaEvent;
    }

    public void setVirologyQaEvent(String virologyQaEvent) {
        this.virologyQaEvent = virologyQaEvent;
    }

    public String getVirologyEidQaEvent() {
        return virologyEidQaEvent;
    }

    public void setVirologyEidQaEvent(String virologyEidQaEvent) {
        this.virologyEidQaEvent = virologyEidQaEvent;
    }

    public String getVirologyVlQaEvent() {
        return virologyVlQaEvent;
    }

    public void setVirologyVlQaEvent(String virologyVlQaEvent) {
        this.virologyVlQaEvent = virologyVlQaEvent;
    }

    public String getSerologyQaEvent() {
        return serologyQaEvent;
    }

    public void setSerologyQaEvent(String serologyQaEvent) {
        this.serologyQaEvent = serologyQaEvent;
    }

    public String getImmunologyQaEvent() {
        return immunologyQaEvent;
    }

    public void setImmunologyQaEvent(String immunologyQaEvent) {
        this.immunologyQaEvent = immunologyQaEvent;
    }

    public String getHematologyQaEvent() {
        return hematologyQaEvent;
    }

    public void setHematologyQaEvent(String hematologyQaEvent) {
        this.hematologyQaEvent = hematologyQaEvent;
    }

    /**
     * @param sample
     * @return
     */
    public String getAllQaEvents() {
        return allQaEvents;
    }

    public void setAllQaEvents(String allQaEvents) {
        this.allQaEvents = allQaEvents;
    }

    public Map<String, String> getPreviousResultMap() {
        return previousResultMap;
    }

    public void setPreviousResultMap(Map<String, String> previousResultMap) {
        this.previousResultMap = previousResultMap;
    }

    public void getSampleQaEventItems(Sample sample) {
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
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.Reception")) {
                    receptionQaEvent = receptionQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : receptionQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                }

                if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.Biochemistry")) {
                    biochemistryQaEvent = biochemistryQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : biochemistryQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                } else if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.Virology")) {
                    virologyQaEvent = virologyQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : virologyQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                } else if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.VL")) {
                    virologyVlQaEvent = virologyVlQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : virologyVlQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                } else if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.EID")) {
                    virologyEidQaEvent = virologyEidQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : virologyEidQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                } else if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.Serology")) {
                    serologyQaEvent = serologyQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : serologyQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                } else if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.Immunology")) {
                    immunologyQaEvent = immunologyQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : immunologyQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                } else if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION).equals("testSection.Hematology")) {
                    hematologyQaEvent = hematologyQaEvent == null ? qa.getQAEvent().getLocalizedName()
                            : hematologyQaEvent + " , " + qa.getQAEvent().getLocalizedName();
                }
            }
        }
    }

    public void getSampleQaEvents(Sample sample) {
        sampleQAEventList = sampleQaEventService.getSampleQaEventsBySample(sample);
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
