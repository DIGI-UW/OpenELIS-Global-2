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
 * <p>Contributor(s): ITECH, University of Washington, Seattle WA.
 */
package org.openelisglobal.sampleqaevent.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;

/**
 * $Header$
 *
 * @author Diane Benz
 * @date created 06/12/2008
 * @version $Revision$ bugzilla 2510
 */
public interface SampleQaEventDAO extends BaseDAO<SampleQaEvent, String> {

    // public boolean insertData(SampleQaEvent sampleQaEvent) throws
    // LIMSRuntimeException;

    // public void deleteData(List sampleQaEvents) throws LIMSRuntimeException;

    void getData(SampleQaEvent sampleQaEvent) throws LIMSRuntimeException;

    // public void updateData(SampleQaEvent sampleQaEvent) throws
    // LIMSRuntimeException;

    List<SampleQaEvent> getSampleQaEventsBySample(SampleQaEvent sampleQaEvent) throws LIMSRuntimeException;

    List<SampleQaEvent> getSampleQaEventsBySample(Sample sample) throws LIMSRuntimeException;

    SampleQaEvent getSampleQaEventBySampleAndQaEvent(SampleQaEvent sampleQaEvent) throws LIMSRuntimeException;

    List<SampleQaEvent> getSampleQaEventsByUpdatedDate(Date lowDate, Date highDate) throws LIMSRuntimeException;

    List<SampleQaEvent> getAllUncompleatedEvents() throws LIMSRuntimeException;

    SampleQaEvent getData(String sampleQaEventId) throws LIMSRuntimeException;
}
