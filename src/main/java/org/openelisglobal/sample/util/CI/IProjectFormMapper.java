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

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.sample.util.CI.BaseProjectFormMapper.TypeOfSampleTests;
import org.openelisglobal.sample.util.CI.form.IProjectForm;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Provide information about mapping from the *entryByProject JSP forms and
 * appropriate data structures
 *
 * @author Laura Nixon, Paul A. Hill (pahill@uw.edu)
 * @since June 20, 2010
 */
public interface IProjectFormMapper {

    /**
     * Find all of the appropriate tests which have been requested
     *
     * @return list of Type Of Sample and the tests requested OR null if the right
     *         sample type was not selected.
     */
    ArrayList<TypeOfSampleTests> getTypeOfSampleTests();

    String getProjectCode();

    ProjectForm getProjectForm();

    String getOrganizationId();

    String getSiteSubjectNumber();

    /**
     * @return a list of observation histories which occur on the form once.
     */
    List<ObservationHistory> readObservationHistories(ObservationData od);

    /**
     * A Map of lists of multi-valued observation history entities, so that even if
     * a list is 100% empty, the key tells the caller what type was supposed to be
     * in the list (so the caller can clean up old existing entities of this type
     * from the DB.
     *
     * @return map<ObservationHistoryTypeId, List<ObservationHistory>>
     */
    Map<String, List<ObservationHistory>> readObservationHistoryLists(ObservationData od);

    /**
     * Sometimes we want to push things into the patient record (and its children),
     * but sometimes we don't. Override this to change that behavior.
     *
     * @return
     */
    boolean getShouldPopulatePatient();

    /**
     * Second Entry is a CI (RetroCI) procedure where data is always entered twice
     * and verified to be the same.
     *
     * @param request
     * @return
     */
    boolean isSecondEntry(HttpServletRequest request);

    /**
     * @return the collection date from the form data.
     */
    String getCollectionDate();

    /**
     * @return the collection date from the form data.
     */
    String getCollectionTime();

    /**
     * @return the known DB id if any for the patient.
     */
    String getPatientId();

    /**
     * @return the known DB id if any for the patient.
     */
    String getSampleId();

    /**
     * @return the received date from the form data.
     */
    String getReceivedDate();

    /**
     * @return the received time from the form data.
     */
    String getReceivedTime();

    /**
     * @param b are we working with a patient form or a sample form?
     */
    void setPatientForm(boolean b);

    void setProjectData(ProjectData projectData);

    TypeOfSample getTypeOfSample(String typeName);

    ProjectData getProjectData();

    /** */
    IProjectForm getForm();
}
