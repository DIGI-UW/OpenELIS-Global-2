package org.openelisglobal.organization.service;

import org.openelisglobal.organization.valueholder.Organization;

/**
 * Which organization record is this laboratory. The record's id is kept in the
 * site setting {@code facilityOrganizationId}, written by the system and listed
 * on no settings screen, so the record stays this laboratory whatever an
 * administrator renames on it (name, short name, address, types).
 */
public interface ThisLaboratoryService {

    String SETTING = "facilityOrganizationId";

    /** Short name the system gives the record when it creates it. */
    String FACILITY_ORG_SHORT_NAME = "FACILITY_ORG";

    /**
     * This laboratory's organization record, or null before the system created it.
     */
    Organization find();

    boolean isThisLaboratory(Organization organization);

    /** Marks the record as this laboratory. */
    void record(Organization organization);
}
