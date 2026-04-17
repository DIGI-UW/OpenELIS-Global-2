package org.openelisglobal.scheduler.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;

@CrossDomainService(callers = "scheduler configuration — internal infrastructure")
public interface CronSchedulerService extends BaseObjectService<CronScheduler, String> {

    CronScheduler getCronScheduleByJobName(String jobName);
}
