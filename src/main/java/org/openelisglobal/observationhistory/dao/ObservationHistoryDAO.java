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
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */

package org.openelisglobal.observationhistory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Record of survey/interview questions. First used for Cote d'Ivoire RetroCI
 * Study forms.
 *
 * @author pahill
 * @since 2010-04-16
 */
public interface ObservationHistoryDAO extends BaseDAO<ObservationHistory, String> {

    // public boolean insertData(ObservationHistory observation) throws
    // LIMSRuntimeException;

    // public void updateData(ObservationHistory observation) throws
    // LIMSRuntimeException;

    // public void insertOrUpdateData(ObservationHistory observation) throws
    // LIMSRuntimeException;

    List<ObservationHistory> getAll(Patient patient, Sample sample);

    /**
     * Find all observations historys of a particular type
     *
     * @param patient                  the patient
     * @param sample                   the sample
     * @param observationHistoryTypeId the TYPE ID of the O.H. entry.
     * @return a List
     */
    List<ObservationHistory> getAll(Patient patient, Sample sample, String observationHistoryTypeId);

    // public void deleteAll(List<ObservationHistory> entities) throws
    // LIMSRuntimeException;

    List<ObservationHistory> getObservationHistoryByDictonaryValues(String dictionaryValue) throws LIMSRuntimeException;

    List<ObservationHistory> getObservationHistoriesBySampleItemId(String sampleItemId) throws LIMSRuntimeException;

    List<ObservationHistory> getObservationHistoriesBySampleId(String sampleId) throws LIMSRuntimeException;

    List<ObservationHistory> getObservationHistoriesByPatientIdAndType(String patientId,
            String observationHistoryTypeId) throws LIMSRuntimeException;

    ObservationHistory getObservationHistoriesBySampleIdAndType(String sampleId, String observationHistoryTypeId)
            throws LIMSRuntimeException;

    ObservationHistory getById(ObservationHistory observation) throws LIMSRuntimeException;

    List<ObservationHistory> getObservationHistoriesByValueAndType(String value, String typeId, String valueType)
            throws LIMSRuntimeException;
}
