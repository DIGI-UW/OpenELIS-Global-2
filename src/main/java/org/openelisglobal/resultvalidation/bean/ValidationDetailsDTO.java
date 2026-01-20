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
package org.openelisglobal.resultvalidation.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for on-demand validation details data (History, QC, Method/Reagents,
 * Order Info, Attachments) Used for expandable row details that are fetched
 * when needed
 */
public class ValidationDetailsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<PreviousResult> previousResults = new ArrayList<>();
    private List<QCResult> qcData = new ArrayList<>();
    private List<ReagentLot> reagentLots = new ArrayList<>();
    private OrderInfo orderInfo;
    private List<Attachment> attachments = new ArrayList<>();
    private DeltaCheck deltaCheck;

    public ValidationDetailsDTO() {
    }

    public static class PreviousResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private String value;
        private String status; // normal, abnormal, low-normal, high-normal

        public PreviousResult() {
        }

        public PreviousResult(String date, String value, String status) {
            this.date = date;
            this.value = value;
            this.status = status;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class QCResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String level;
        private String expected;
        private String actual;
        private String status; // pass, fail
        private String cv; // coefficient of variation

        public QCResult() {
        }

        public QCResult(String level, String expected, String actual, String status, String cv) {
            this.level = level;
            this.expected = expected;
            this.actual = actual;
            this.status = status;
            this.cv = cv;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getExpected() {
            return expected;
        }

        public void setExpected(String expected) {
            this.expected = expected;
        }

        public String getActual() {
            return actual;
        }

        public void setActual(String actual) {
            this.actual = actual;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCv() {
            return cv;
        }

        public void setCv(String cv) {
            this.cv = cv;
        }
    }

    public static class ReagentLot implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String lot;
        private String expires;
        private String status; // ok, expiring-soon, expired

        public ReagentLot() {
        }

        public ReagentLot(String name, String lot, String expires, String status) {
            this.name = name;
            this.lot = lot;
            this.expires = expires;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLot() {
            return lot;
        }

        public void setLot(String lot) {
            this.lot = lot;
        }

        public String getExpires() {
            return expires;
        }

        public void setExpires(String expires) {
            this.expires = expires;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class OrderInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String clinician;
        private String clinicianPhone;
        private String department;
        private String priority;
        private String collectionDate;
        private String receivedDate;
        private String clinicalHistory;
        private String diagnosis;
        private String fastingStatus;
        private String medicationList;

        public OrderInfo() {
        }

        public String getClinician() {
            return clinician;
        }

        public void setClinician(String clinician) {
            this.clinician = clinician;
        }

        public String getClinicianPhone() {
            return clinicianPhone;
        }

        public void setClinicianPhone(String clinicianPhone) {
            this.clinicianPhone = clinicianPhone;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getCollectionDate() {
            return collectionDate;
        }

        public void setCollectionDate(String collectionDate) {
            this.collectionDate = collectionDate;
        }

        public String getReceivedDate() {
            return receivedDate;
        }

        public void setReceivedDate(String receivedDate) {
            this.receivedDate = receivedDate;
        }

        public String getClinicalHistory() {
            return clinicalHistory;
        }

        public void setClinicalHistory(String clinicalHistory) {
            this.clinicalHistory = clinicalHistory;
        }

        public String getDiagnosis() {
            return diagnosis;
        }

        public void setDiagnosis(String diagnosis) {
            this.diagnosis = diagnosis;
        }

        public String getFastingStatus() {
            return fastingStatus;
        }

        public void setFastingStatus(String fastingStatus) {
            this.fastingStatus = fastingStatus;
        }

        public String getMedicationList() {
            return medicationList;
        }

        public void setMedicationList(String medicationList) {
            this.medicationList = medicationList;
        }
    }

    public static class Attachment implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private String type; // pdf, image, etc.
        private String size;
        private String uploadedBy;
        private String uploadedAt;
        private String source; // order, result

        public Attachment() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getUploadedBy() {
            return uploadedBy;
        }

        public void setUploadedBy(String uploadedBy) {
            this.uploadedBy = uploadedBy;
        }

        public String getUploadedAt() {
            return uploadedAt;
        }

        public void setUploadedAt(String uploadedAt) {
            this.uploadedAt = uploadedAt;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    public static class DeltaCheck implements Serializable {
        private static final long serialVersionUID = 1L;
        private String previous;
        private String change;
        private String threshold;

        public DeltaCheck() {
        }

        public DeltaCheck(String previous, String change, String threshold) {
            this.previous = previous;
            this.change = change;
            this.threshold = threshold;
        }

        public String getPrevious() {
            return previous;
        }

        public void setPrevious(String previous) {
            this.previous = previous;
        }

        public String getChange() {
            return change;
        }

        public void setChange(String change) {
            this.change = change;
        }

        public String getThreshold() {
            return threshold;
        }

        public void setThreshold(String threshold) {
            this.threshold = threshold;
        }
    }

    public List<PreviousResult> getPreviousResults() {
        return previousResults;
    }

    public void setPreviousResults(List<PreviousResult> previousResults) {
        this.previousResults = previousResults;
    }

    public List<QCResult> getQcData() {
        return qcData;
    }

    public void setQcData(List<QCResult> qcData) {
        this.qcData = qcData;
    }

    public List<ReagentLot> getReagentLots() {
        return reagentLots;
    }

    public void setReagentLots(List<ReagentLot> reagentLots) {
        this.reagentLots = reagentLots;
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public DeltaCheck getDeltaCheck() {
        return deltaCheck;
    }

    public void setDeltaCheck(DeltaCheck deltaCheck) {
        this.deltaCheck = deltaCheck;
    }
}
