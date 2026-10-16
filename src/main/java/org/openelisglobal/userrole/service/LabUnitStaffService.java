package org.openelisglobal.userrole.service;

import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Who holds a role in a lab unit: the role granted on that unit, or on all lab
 * units. Used to offer and to check the staff a case can be assigned to.
 */
public interface LabUnitStaffService {

    /** Active users holding the role in the lab unit, by display name. */
    List<IdValuePair> getActiveUsersWithRole(String roleName, String labUnitName);

    boolean hasRole(String systemUserId, String roleName, String labUnitName);

    /**
     * Refuses a newly chosen technician who lacks the Results role in the lab unit.
     * Keeping the technician already on the case is always allowed, so cases saved
     * before this check stay editable.
     *
     * @throws IllegalArgumentException if the new technician isn't allowed
     */
    void checkTechnician(String newTechnicianId, SystemUser currentTechnician, String labUnitName);
}
