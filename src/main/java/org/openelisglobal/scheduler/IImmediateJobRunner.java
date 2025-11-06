package org.openelisglobal.scheduler;

import org.quartz.Scheduler;

public interface IImmediateJobRunner {

    void runNow(Scheduler scheduler, String jobName);
}
