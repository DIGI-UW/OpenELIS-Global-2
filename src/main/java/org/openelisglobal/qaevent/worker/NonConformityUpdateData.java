/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License();
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.qaevent.worker;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.qaevent.form.NonConformityForm;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;

public class NonConformityUpdateData {

    private String sampleId;
    private String labNo;
    private String currentSysUserId;
    private String patientId;
    private Boolean newSTNumber;
    private String STNumber;
    private Boolean newSubject;
    private String subjectNo;
    private Boolean newNationalId;
    private String nationalId;
    private Boolean newDoctor = false;
    private String doctor;
    private String requesterSpecimanID;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterPhoneNumber;
    private String requesterStreetAddress;
    private String requesterVillage;
    private String requesterCommune;
    private String requesterDepartment;
    private Boolean newService = false;
    private String service;
    private String newServiceName;
    private String receivedDate;
    private String receivedTime;
    private String projectId;
    private List<QaEventItem> qaEvents;
    private Boolean newNoteText;
    private String noteText;

    public NonConformityUpdateData(NonConformityForm form, String currentUserId) {

        currentSysUserId = currentUserId;
        sampleId = form.getSampleId();
        labNo = form.getLabNo();
        patientId = form.getPatientId();
        newSubject = form.getSubjectNew();
        subjectNo = form.getSubjectNo();
        newSTNumber = form.getNewSTNumber();
        STNumber = form.getSTNumber();
        newNationalId = form.getNationalIdNew();
        nationalId = form.getNationalId();
        newDoctor = form.getDoctorNew();
        doctor = form.getDoctor();
        requesterSpecimanID = form.getRequesterSampleID();
        requesterFirstName = form.getProviderFirstName();
        requesterLastName = form.getProviderLastName();
        requesterPhoneNumber = form.getProviderWorkPhone();
        requesterStreetAddress = form.getProviderStreetAddress();
        requesterCommune = form.getProviderCommune();
        requesterVillage = form.getProviderCity();
        requesterDepartment = form.getProviderDepartment();
        newService = form.getServiceNew();
        service = form.getService();
        newServiceName = form.getNewServiceName();
        receivedDate = form.getDate();
        receivedTime = form.getTime();
        projectId = form.getProjectId();
        qaEvents = form.getQaEvents();
        newNoteText = form.getCommentNew();
        noteText = form.getComment();
    }

    public Boolean getNewNationalId() {
        return newNationalId;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getLabNo() {
        return labNo;
    }

    public String getCurrentSysUserId() {
        return currentSysUserId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getSubjectNo() {
        return subjectNo;
    }

    public Boolean getNewDoctor() {
        return newDoctor;
    }

    public String getDoctor() {
        return doctor;
    }

    public String getRequesterSpecimanID() {
        return requesterSpecimanID;
    }

    public String getRequesterFirstName() {
        return requesterFirstName;
    }

    public String getRequesterLastName() {
        return requesterLastName;
    }

    public String getRequesterPhoneNumber() {
        return requesterPhoneNumber;
    }

    public boolean noRequesterInformation() {
        return (GenericValidator.isBlankOrNull(requesterFirstName)
                && GenericValidator.isBlankOrNull(requesterPhoneNumber)
                && GenericValidator.isBlankOrNull(requesterLastName)
                && GenericValidator.isBlankOrNull(requesterSpecimanID));
    }

    public Boolean getNewService() {
        return newService;
    }

    public String getService() {
        return service;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public String getProjectId() {
        return projectId;
    }

    public List<QaEventItem> getQaEvents() {
        return qaEvents;
    }

    public Boolean getNewNoteText() {
        return newNoteText;
    }

    public String getNoteText() {
        return noteText;
    }

    public Boolean getNewSubject() {
        return newSubject;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public String getNewServiceName() {
        return newServiceName;
    }

    public String getRequesterStreetAddress() {
        return requesterStreetAddress;
    }

    public String getRequesterVillage() {
        return requesterVillage;
    }

    public String getRequesterCommune() {
        return requesterCommune;
    }

    public String getRequesterDepartment() {
        return requesterDepartment;
    }

    public Boolean getNewSTNumber() {
        return newSTNumber;
    }

    public String getSTNumber() {
        return STNumber;
    }
}
