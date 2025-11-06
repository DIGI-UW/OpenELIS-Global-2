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
package org.openelisglobal.sample.util.CI;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.util.CI.form.IProjectForm;

public class ProjectFormMapperFactory {

    private static final String InitialARV = "InitialARV_Id";
    private static final String FollowUpARV = "FollowUpARV_Id";
    private static final String EID = "EID_Id";
    private static final String VL = "VL_Id";
    private static final String RTN = "RTN_Id";
    private static final String IND = "Indeterminate_Id";
    private static final String SPE = "Special_Request_Id";
    private static final String RT = "Recency_Id";
    private static final String HPV = "HPV_Id";

    public IProjectFormMapper getProjectInitializer(String projectFormId, IProjectForm form)
            throws LIMSRuntimeException {

        if (projectFormId.equals(InitialARV) || projectFormId.equals(FollowUpARV) || projectFormId.equals(VL)) {
            return new ARVFormMapper(projectFormId, form);
        } else if (projectFormId.equals(EID)) {
            return new EIDFormMapper(projectFormId, form);
        } else if (projectFormId.equals(RTN)) {
            return new RTNFormMapper(projectFormId, form);
        } else if (projectFormId.equals(IND)) {
            return new INDFormMapper(projectFormId, form);
        } else if (projectFormId.equals(SPE)) {
            return new SPEFormMapper(projectFormId, form);
        } else if (projectFormId.equals(RT)) {
            return new RecencyFormMapper(projectFormId, form);
        } else if (projectFormId.equals(HPV)) {
            return new HPVFormMapper(projectFormId, form);
        }

        throw new LIMSRuntimeException(
                "ProjectFormMapperFactory: Unable to find project initializer for " + projectFormId);
    }
}
