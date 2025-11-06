package org.openelisglobal.scheduler.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;

public interface CronSchedulerService extends BaseObjectService<CronScheduler, String> {

    CronScheduler getCronScheduleByJobName(String jobName);
}
