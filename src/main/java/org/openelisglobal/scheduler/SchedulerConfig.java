package org.openelisglobal.scheduler;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.MalariaSurveilance.MalariaSurveilanceJob;
import org.openelisglobal.dataexchange.aggregatereporting.AggregateReportJob;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.spring.util.SpringContext;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    private static long DEFAULT_RESEND_TIME = 30L;

    private static final String NEVER = "never";

    private static final Map<String, Class<? extends Job>> scheduleJobMap;

    static {
        scheduleJobMap = new HashMap<>();
        scheduleJobMap.put("sendSiteIndicators", AggregateReportJob.class);
        scheduleJobMap.put("sendMalariaSurviellanceReport", MalariaSurveilanceJob.class);
    }

    @Autowired
    private CronSchedulerService cronSchedulerService;

    private Scheduler reloadableScheduler;

    @Bean("resultsResendTime")
    public long getResultsResendTimeMillis() {
        long period = DEFAULT_RESEND_TIME;

        String reportInterval = ConfigurationProperties.getInstance().getPropertyValue(Property.resultsResendTime);
        if (!GenericValidator.isBlankOrNull(reportInterval)) {
            try {
                period = Long.parseLong(reportInterval);
            } catch (NumberFormatException e) {
                LogEvent.logError("Unable to parse " + reportInterval, e);
            }
        }
        return period * 60 * 1000;
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(10);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // for Spring @Scheduled tasks
        taskRegistrar.setScheduler(taskExecutor());

        // for reloadable tasks using quartz scheduler
        try {
            reloadableScheduler = StdSchedulerFactory.getDefaultScheduler();
            addReloadableCronSchedulers(reloadableScheduler);
        } catch (SchedulerException e) {
            LogEvent.logError(e);
        }
    }

    public synchronized void reloadSchedules() {
        try {
            reloadableScheduler.shutdown();
            reloadableScheduler = StdSchedulerFactory.getDefaultScheduler();

            addReloadableCronSchedulers(reloadableScheduler);
        } catch (SchedulerException e) {
            LogEvent.logError("A scheduler excecption occured while reloading", e);
        }
    }

    private synchronized void addReloadableCronSchedulers(Scheduler reloadableScheduler) {
        List<CronScheduler> schedulers = cronSchedulerService.getAll();
        for (CronScheduler cronScheduler : schedulers) {
            try {
                addOrRunSchedule(reloadableScheduler, cronScheduler);
            } catch (SchedulerException | ParseException e) {
                LogEvent.logError("Could not add a cron job to scheduler", e);
            }
        }
    }

    private synchronized void addOrRunSchedule(Scheduler reloadableScheduler, CronScheduler cronScheduler)
            throws SchedulerException, ParseException {
        int currentHour = DateUtil.getCurrentHour();
        int currentMin = DateUtil.getCurrentMinute();

        if (!cronScheduler.getActive() || NEVER.equals(cronScheduler.getCronStatement())) {
            return;
        }

        String jobName = cronScheduler.getJobName();
        LogEvent.logInfo(this.getClass().getSimpleName(), "addOrRunSchedule", "Adding cron job: " + jobName);

        Class<? extends Job> targetJob = scheduleJobMap.get(jobName);

        if (targetJob == null) {
            return;
        }

        JobDetail job = newJob(targetJob).withIdentity(jobName + "Job", jobName).build();

        Trigger trigger = newTrigger().withIdentity(jobName + "Trigger", jobName)
                .withSchedule(cronSchedule(cronScheduler.getCronStatement())).forJob(jobName + "Job", jobName).build();

        reloadableScheduler.scheduleJob(job, trigger);

        String[] cronParts = cronScheduler.getCronStatement().split(" ");

        try {
            int cronHour = Integer.parseInt(cronParts[2]);
            int cronMinutes = Integer.parseInt(cronParts[1]);

            if (cronHour < currentHour || (cronHour == currentHour && cronMinutes < currentMin)) {
                IImmediateJobRunner runner = SpringContext.getBean(IImmediateJobRunner.class);
                runner.runNow(reloadableScheduler, jobName);
            }
        } catch (NumberFormatException e) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "addOrRunSchedule",
                    "Malformed cron statement." + cronScheduler.getCronStatement());
        }
    }
}
