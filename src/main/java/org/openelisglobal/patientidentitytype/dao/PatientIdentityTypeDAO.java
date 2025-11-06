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
package org.openelisglobal.patientidentitytype.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;

public interface PatientIdentityTypeDAO extends BaseDAO<PatientIdentityType, String> {

    // public void insertData(PatientIdentityType patientIdenityType) throws
    // LIMSRuntimeException;

    public List<PatientIdentityType> getAllPatientIdenityTypes() throws LIMSRuntimeException;

    public PatientIdentityType getNamedIdentityType(String name) throws LIMSRuntimeException;

    boolean duplicatePatientIdentityTypeExists(PatientIdentityType patientIdentityType) throws LIMSRuntimeException;
}
