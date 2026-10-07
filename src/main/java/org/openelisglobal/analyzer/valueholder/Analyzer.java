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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.hibernateConverter.StringListConverter;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer")
@DynamicUpdate
public class Analyzer extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_seq_gen")
    @GenericGenerator(name = "analyzer_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "is_active", length = 1)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_binding_revision_id")
    private AnalyzerSiteBindingRevision siteBindingRevision;

    @Column(name = "bridge_connection_id", length = 255)
    private String bridgeConnectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_activation_record_id")
    private AnalyzerActivationRecord latestActivationRecord;

    @Column(name = "test_unit_ids", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> testUnitIds = new ArrayList<>();

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private AnalyzerStatus status = AnalyzerStatus.SETUP;

    @Column(name = "last_activated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivatedDate;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public AnalyzerSiteBindingRevision getSiteBindingRevision() {
        return siteBindingRevision;
    }

    public void setSiteBindingRevision(AnalyzerSiteBindingRevision siteBindingRevision) {
        this.siteBindingRevision = siteBindingRevision;
    }

    public String getBridgeConnectionId() {
        return bridgeConnectionId;
    }

    public void setBridgeConnectionId(String bridgeConnectionId) {
        this.bridgeConnectionId = bridgeConnectionId;
    }

    public AnalyzerActivationRecord getLatestActivationRecord() {
        return latestActivationRecord;
    }

    public void setLatestActivationRecord(AnalyzerActivationRecord latestActivationRecord) {
        this.latestActivationRecord = latestActivationRecord;
    }

    public AnalyzerProfileBinding getPinnedProfileBinding() {
        if (siteBindingRevision == null || siteBindingRevision.getSiteBinding() == null) {
            return null;
        }
        return siteBindingRevision.getSiteBinding().getProfileBinding();
    }

    public List<String> getTestUnitIds() {
        return testUnitIds;
    }

    public void setTestUnitIds(List<String> testUnitIds) {
        this.testUnitIds = testUnitIds != null ? testUnitIds : new ArrayList<>();
    }

    public AnalyzerStatus getStatus() {
        return status;
    }

    public void setStatus(AnalyzerStatus status) {
        this.status = status;
    }

    public Date getLastActivatedDate() {
        return lastActivatedDate;
    }

    public void setLastActivatedDate(Date lastActivatedDate) {
        this.lastActivatedDate = lastActivatedDate;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    /**
     * Returns the fhirUuid as a String, or null if not yet assigned. Use
     * {@link #ensureFhirUuid()} to generate and persist a UUID if needed.
     */
    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }

    /**
     * Ensures this analyzer has a stable FHIR UUID. If none exists, generates one.
     * Callers should persist the entity after calling this in a write transaction.
     */
    public String ensureFhirUuid() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
        return fhirUuid.toString();
    }

    /**
     * Enum for analyzer unified status field. Values must match database
     * constraint: INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE
     */
    public enum AnalyzerStatus {
        INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE
    }
}
