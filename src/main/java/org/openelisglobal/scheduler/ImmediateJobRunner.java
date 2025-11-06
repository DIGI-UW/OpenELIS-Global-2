package org.openelisglobal.scheduler;

import org.openelisglobal.common.log.LogEvent;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ImmediateJobRunner implements IImmediateJobRunner {

    @Override
    @Async
    public void runNow(Scheduler scheduler, String jobName) {
        try {
            synchronized (scheduler) {
                if (!scheduler.isShutdown()) {
                    scheduler.triggerJob(new JobKey(jobName + "Job", jobName));
                }
            }
        } catch (SchedulerException e) {
            LogEvent.logDebug(e);
        }
    }
}
