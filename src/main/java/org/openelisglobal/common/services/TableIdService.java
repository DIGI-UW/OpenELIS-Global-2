/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.common.services;

import jakarta.annotation.PostConstruct;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** */
@Service
public class TableIdService {

    private static TableIdService INSTANCE;

    // address parts
    public String ADDRESS_COMMUNE_ID;
    public String ADDRESS_FAX_ID;
    public String ADDRESS_PHONE_ID;
    public String ADDRESS_STREET_ID;
    public String ADDRESS_VILLAGE_ID;
    public String ADDRESS_DEPARTMENT_ID;

    // requester type
    public long ORGANIZATION_REQUESTER_TYPE_ID;
    public long PROVIDER_REQUESTER_TYPE_ID;

    // organization type
    public String REFERRING_ORG_TYPE_ID;
    public String REFERRING_ORG_DEPARTMENT_TYPE_ID;

    // Observations types
    public String DOCTOR_OBSERVATION_TYPE_ID;
    public String SERVICE_OBSERVATION_TYPE_ID;

    // Patient identity
    public String PATIENT_SUBJECT_IDENTITY;
    public String PATIENT_ST_IDENTITY;

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;
    @Autowired
    private RequesterTypeService requesterTypeService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private OrganizationTypeService organizationTypeService;
    @Autowired
    private PatientIdentityTypeService patientIdentityTypeService;
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @PostConstruct
    private void registerInstance() {
        INSTANCE = this;
    }

    @PostConstruct
    private void initialize() {
        // Run all the reference-data lookups in one committed transaction.
        // @Transactional is
        // ignored on @PostConstruct (the bean proxy isn't in place yet), so without
        // this the
        // ~15 reads below run on a session that is never committed/closed, leaving a
        // connection idle-in-transaction that holds reference-table locks and blocks
        // fixture
        // TRUNCATEs in tests (#3711).
        new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> initializeIds());
    }

    private void initializeIds() {
        RequesterType type = requesterTypeService.getRequesterTypeByName("organization");
        if (type != null) {
            ORGANIZATION_REQUESTER_TYPE_ID = Long.parseLong(type.getId());
        }
        type = requesterTypeService.getRequesterTypeByName("provider");
        if (type != null) {
            PROVIDER_REQUESTER_TYPE_ID = Long.parseLong(type.getId());
        }

        OrganizationType orgType = organizationTypeService.getOrganizationTypeByName("referring clinic");
        REFERRING_ORG_TYPE_ID = orgType != null ? orgType.getId() : "";

        orgType = organizationTypeService.getOrganizationTypeByName("dept");
        REFERRING_ORG_DEPARTMENT_TYPE_ID = orgType != null ? orgType.getId() : "";

        AddressPart part = addressPartService.getAddresPartByName("commune");
        ADDRESS_COMMUNE_ID = part == null ? "" : part.getId();

        part = addressPartService.getAddresPartByName("village");
        ADDRESS_VILLAGE_ID = part == null ? "" : part.getId();

        part = addressPartService.getAddresPartByName("department");
        ADDRESS_DEPARTMENT_ID = part == null ? "" : part.getId();

        part = addressPartService.getAddresPartByName("fax");
        ADDRESS_FAX_ID = part == null ? "" : part.getId();

        part = addressPartService.getAddresPartByName("phone");
        ADDRESS_PHONE_ID = part == null ? "" : part.getId();

        part = addressPartService.getAddresPartByName("street");
        ADDRESS_STREET_ID = part == null ? "" : part.getId();

        PatientIdentityType patientType = patientIdentityTypeService.getNamedIdentityType("SUBJECT");
        PATIENT_SUBJECT_IDENTITY = patientType != null ? patientType.getId() : "";

        patientType = patientIdentityTypeService.getNamedIdentityType("ST");
        PATIENT_ST_IDENTITY = patientType != null ? patientType.getId() : "";

        DOCTOR_OBSERVATION_TYPE_ID = getOHTypeIdByName("nameOfDoctor");
        SERVICE_OBSERVATION_TYPE_ID = getOHTypeIdByName("service");
    }

    public static TableIdService getInstance() {
        return INSTANCE;
    }

    private String getOHTypeIdByName(String name) {
        ObservationHistoryType oht = observationHistoryTypeService.getByName(name);
        return (oht == null) ? null : oht.getId();
    }
}
