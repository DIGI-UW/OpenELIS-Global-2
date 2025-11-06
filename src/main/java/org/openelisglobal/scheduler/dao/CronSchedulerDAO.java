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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.scheduler.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.scheduler.valueholder.CronScheduler;

public interface CronSchedulerDAO extends BaseDAO<CronScheduler, String> {
    // public List<CronScheduler> getAllCronSchedules() throws LIMSRuntimeException;

    // public CronScheduler getCronScheduleByJobName(String jobName) throws
    // LIMSRuntimeException;

    // public Optional<CronScheduler> update(CronScheduler cronScheduler) throws
    // LIMSRuntimeException;

    // public CronScheduler getCronScheduleById(String schedulerId) throws
    // LIMSRuntimeException;
}
