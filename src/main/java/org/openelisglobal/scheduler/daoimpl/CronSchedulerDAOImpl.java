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
package org.openelisglobal.scheduler.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.scheduler.dao.CronSchedulerDAO;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CronSchedulerDAOImpl extends BaseDAOImpl<CronScheduler, String> implements CronSchedulerDAO {

    public CronSchedulerDAOImpl() {
        super(CronScheduler.class);
    }

    // @Override
    // public List<CronScheduler> getAllCronSchedules() throws LIMSRuntimeException
    // {
    // String sql = "from CronScheduler";
    //
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    //
    // List<CronScheduler> schedulers = query.list();
    // // closeSession(); // CSL remove old
    // return schedulers;
    // } catch (HibernateException e) {
    // handleException(e, "getAllCronSchedules");
    // }
    //
    // return null;
    // }

    // @Override
    // public CronScheduler getCronScheduleByJobName(String jobName) throws
    // LIMSRuntimeException {
    // String sql = "from CronScheduler cs where cs.jobName = :jobName";
    //
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setString("jobName", jobName);
    // CronScheduler scheduler = (CronScheduler) query.uniqueResult();
    // // closeSession(); // CSL remove old
    // return scheduler;
    // } catch (HibernateException e) {
    // handleException(e, "getCronScheduleByJobName");
    // }
    //
    // return null;
    // }

    // @Override
    // public String insert(CronScheduler cronSchedule) throws LIMSRuntimeException
    // {
    // try {
    // String id = (String) entityManager.unwrap(Session.class).save(cronSchedule);
    // cronSchedule.setId(id);
    // auditDAO.saveNewHistory(cronSchedule, cronSchedule.getSysUserId(),
    // "QUARTZ_CRON_SCHEDULER");
    // // closeSession(); // CSL remove old
    // return id;
    // } catch (HibernateException e) {
    // handleException(e, "insert");
    // }
    // return null;
    // }

    // @Override
    // public Optional<CronScheduler> update(CronScheduler cronSchedule) throws
    // LIMSRuntimeException {
    // CronScheduler oldData = readCronScheduler(cronSchedule.getId());
    //
    // try {
    //
    // auditDAO.saveHistory(cronSchedule, oldData, cronSchedule.getSysUserId(),
    // IActionConstants.AUDIT_TRAIL_UPDATE, "QUARTZ_CRON_SCHEDULER");
    //
    // entityManager.unwrap(Session.class).merge(cronSchedule);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // // entityManager.unwrap(Session.class).evict // CSL remove old(cronSchedule);
    // // entityManager.unwrap(Session.class).refresh // CSL remove
    // old(cronSchedule);
    // } catch (RuntimeException e) {
    // handleException(e, "update");
    // }
    // return Optional.ofNullable(cronSchedule);
    // }

    // public CronScheduler readCronScheduler(String idString) throws
    // LIMSRuntimeException {
    //
    // try {
    // CronScheduler data =
    // entityManager.unwrap(Session.class).get(CronScheduler.class, idString);
    // // closeSession(); // CSL remove old
    // return data;
    // } catch (HibernateException e) {
    // handleException(e, "readCronScheduler");
    // }
    // return null;
    // }

    // @Override
    // public CronScheduler getCronScheduleById(String schedulerId) throws
    // LIMSRuntimeException {
    // String sql = "from CronScheduler cs where cs.id = :id";
    //
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setInteger("id", Integer.parseInt(schedulerId));
    // CronScheduler scheduler = (CronScheduler) query.uniqueResult();
    // // closeSession(); // CSL remove old
    // return scheduler;
    // } catch (HibernateException e) {
    // handleException(e, "getCronScheduleById");
    // }
    //
    // return null;
    // }
}
