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
package org.openelisglobal.medlab.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Time;
import java.util.Objects;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * EquipmentUsageLog entity for tracking laboratory equipment/instrument usage.
 *
 * <p>
 * Supports accreditation requirements by documenting:
 * <ul>
 * <li>Equipment usage periods (start/end time)</li>
 * <li>Operator identification</li>
 * <li>Purpose of usage (testing, calibration, QC, maintenance)</li>
 * <li>Samples processed count</li>
 * <li>Maintenance activities</li>
 * <li>Error tracking</li>
 * </ul>
 */
@Entity
@Table(name = "equipment_usage_log")
public class EquipmentUsageLog extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_usage_log_seq")
    @SequenceGenerator(name = "equipment_usage_log_seq", sequenceName = "equipment_usage_log_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "analyzer_id", nullable = false)
    private Integer analyzerId;

    @Column(name = "usage_date", nullable = false)
    private Date usageDate;

    @Column(name = "start_time", nullable = false)
    private Time startTime;

    @Column(name = "end_time")
    private Time endTime;

    @Column(name = "operator_id", nullable = false)
    private Integer operatorId;

    @Column(name = "purpose", nullable = false, length = 100)
    private String purpose;

    @Column(name = "samples_processed")
    private Integer samplesProcessed;

    @Column(name = "maintenance_done")
    private Boolean maintenanceDone = false;

    @Column(name = "maintenance_notes", columnDefinition = "TEXT")
    private String maintenanceNotes;

    @Column(name = "error_occurred")
    private Boolean errorOccurred = false;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "fhir_uuid", unique = true)
    private UUID fhirUuid;

    public EquipmentUsageLog() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(Integer analyzerId) {
        this.analyzerId = analyzerId;
    }

    public Date getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(Date usageDate) {
        this.usageDate = usageDate;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public Integer getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Integer getSamplesProcessed() {
        return samplesProcessed;
    }

    public void setSamplesProcessed(Integer samplesProcessed) {
        this.samplesProcessed = samplesProcessed;
    }

    public Boolean getMaintenanceDone() {
        return maintenanceDone;
    }

    public void setMaintenanceDone(Boolean maintenanceDone) {
        this.maintenanceDone = maintenanceDone;
    }

    public String getMaintenanceNotes() {
        return maintenanceNotes;
    }

    public void setMaintenanceNotes(String maintenanceNotes) {
        this.maintenanceNotes = maintenanceNotes;
    }

    public Boolean getErrorOccurred() {
        return errorOccurred;
    }

    public void setErrorOccurred(Boolean errorOccurred) {
        this.errorOccurred = errorOccurred;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    /**
     * Calculates usage duration in minutes.
     *
     * @return duration in minutes, or null if end time not set
     */
    public Long getUsageDurationMinutes() {
        if (endTime == null || startTime == null) {
            return null;
        }
        long startMillis = startTime.getTime();
        long endMillis = endTime.getTime();
        return (endMillis - startMillis) / (1000 * 60);
    }

    /**
     * Determines if usage session is still active (no end time).
     *
     * @return true if session is active
     */
    public boolean isSessionActive() {
        return endTime == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EquipmentUsageLog that = (EquipmentUsageLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
