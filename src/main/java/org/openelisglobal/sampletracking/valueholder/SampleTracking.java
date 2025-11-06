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
package org.openelisglobal.sampletracking.valueholder;

import java.io.Serializable;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * @author AIS view sampletracking is mapped to this valueholder bug 1920 -
 *         standards
 */
// TODO delete unused class
public class SampleTracking extends BaseObject<String> implements Serializable {

    private SampleTracking sampleTrackingId;

    private String id;

    private String patientId;

    private String cliRef;

    private String patientLastName;

    private String patientFirstName;

    // bugzilla 2069
    private String organizationLocalAbbreviation;

    private String orgName;

    private String recdDate;

    private String tosId;

    private String tosDesc;

    private String sosId;

    private String sosDesc;

    private String collDate;

    private String dateofBirth;

    // bugzilla 2455
    private String specOrIsolate;

    public SampleTracking() {
        super();
    }

    // bugzilla 2455
    public String getSpecOrIsolate() {
        return specOrIsolate;
    }

    public void setSpecOrIsolate(String specOrIsolate) {
        this.specOrIsolate = specOrIsolate;
    }

    public void setSampleTrackingId(SampleTracking sampleTrackingId) {
        this.sampleTrackingId = sampleTrackingId;
    }

    public SampleTracking getSampleTrackingId() {
        return sampleTrackingId;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getCliRef() {
        return cliRef;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public String getOrgName() {
        return orgName;
    }

    // bugzilla 2069
    public String getOrganizationLocalAbbreviation() {
        return organizationLocalAbbreviation;
    }

    public String getRecdDate() {
        return recdDate;
    }

    public String getTosId() {
        return tosId;
    }

    public String getTosDesc() {
        return tosDesc;
    }

    public String getSosId() {
        return sosId;
    }

    public String getSosDesc() {
        return sosDesc;
    }

    public String getCollDate() {
        return collDate;
    }

    public String getDateofBirth() {
        return dateofBirth;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setCliRef(String cliRef) {
        this.cliRef = cliRef;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    // bugzilla 2069
    public void setOrganizationLocalAbbreviation(String organizationLocalAbbreviation) {
        this.organizationLocalAbbreviation = organizationLocalAbbreviation;
    }

    public void setRecdDate(String recdDate) {
        this.recdDate = recdDate;
    }

    public void setTosId(String tosId) {
        this.tosId = tosId;
    }

    public void setTosDesc(String tosDesc) {
        this.tosDesc = tosDesc;
    }

    public void setSosId(String sosId) {
        this.sosId = sosId;
    }

    public void setSosDesc(String sosDesc) {
        this.sosDesc = sosDesc;
    }

    public void setCollDate(String collDate) {
        this.collDate = collDate;
    }

    public void setDateofBirth(String dateofBirth) {
        this.dateofBirth = dateofBirth;
    }

//    @Override
//    public String getId() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void setId(String id) {
//        throw new UnsupportedOperationException();
//    }
}
