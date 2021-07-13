/** */
package org.sunbird.common.quartz.scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.PropertiesCache;

/**
 * This class will manage all the Quartz scheduler. We need to call the schedule method at one time.
 * we are calling this method from Util.java class.
 *
 * @author Manzarul
 */
public class SchedulerManager {
  private static LoggerUtil logger = new LoggerUtil(SchedulerManager.class);

  private static final String FILE = "quartz.properties";
  public static Scheduler scheduler = null;
  private static SchedulerManager schedulerManager = null;

  public SchedulerManager() {
    schedule();
  }

  /** This method will register the quartz scheduler job. */
  public void schedule() {
    logger.info(
        "SchedulerManager:schedule: Call to start scheduler jobs - org.sunbird.common.quartz.scheduler.SchedulerManager");

    try {
      Thread.sleep(240000);
      boolean isEmbedded = false;
      Properties configProp = null;
      String embeddVal = System.getenv(JsonKey.SUNBIRD_QUARTZ_MODE);
      if (JsonKey.EMBEDDED.equalsIgnoreCase(embeddVal)) {
        isEmbedded = true;
      } else {
        configProp = setUpClusterMode();
      }
      if (!isEmbedded && configProp != null) {

        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory(configProp);
        logger.info("Quartz scheduler is running in cluster mode.");
        scheduler = schedulerFactory.getScheduler("MyScheduler");

        if (null == scheduler) {
          Thread.sleep(5000);
          scheduler = schedulerFactory.getScheduler();
        }

        String schedulerName = scheduler.getSchedulerName();
        logger.info(
            "Quartz scheduler is running in cluster mode. scheduler Name is: " + schedulerName);
      } else {
        logger.info("Quartz scheduler is running in embedded mode.");
        scheduler = new StdSchedulerFactory().getScheduler();
      }
      String identifier = "NetOps-PC1502295457753";

      scheduleBulkUploadJob(identifier);
      scheduleChannelReg(identifier);
      scheduleShadowUser(identifier);

    } catch (Exception e) {
      logger.error(
          "SchedulerManager:schedule: Error in starting scheduler jobs - org.sunbird.common.quartz.scheduler.SchedulerManager "
              + e.getMessage(),
          e);
    } finally {
      registerShutDownHook();
    }
    logger.info(
        "SchedulerManager:schedule: started scheduler jobs - org.sunbird.common.quartz.scheduler.SchedulerManager");
  }

  public static void scheduleChannelReg(String identifier) {
    // add another job for registering channel to ekstep.
    // 1- create a job and bind with class which is implementing Job
    // interface.
    JobDetail channelRegistrationJob =
        JobBuilder.newJob(ChannelRegistrationScheduler.class)
            .requestRecovery(true)
            .withDescription("Scheduler for channel registration")
            .withIdentity("channelRegistrationScheduler", identifier)
            .build();

    // 2- Create a trigger object that will define frequency of run.
    // It will run only once after server startup
    Trigger channelRegistrationTrigger =
        TriggerBuilder.newTrigger()
            .withIdentity("channelRegistrationScheduler", identifier)
            .withSchedule(SimpleScheduleBuilder.repeatMinutelyForTotalCount(1))
            .build();
    try {
      if (scheduler.checkExists(channelRegistrationJob.getKey())) {
        scheduler.deleteJob(channelRegistrationJob.getKey());
      }
      scheduler.scheduleJob(channelRegistrationJob, channelRegistrationTrigger);
      scheduler.start();
      logger.info("SchedulerManager:scheduleChannelReg: channelRegistration schedular started");
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  private void scheduleBulkUploadJob(String identifier) {
    // add another job for verifying the bulk upload part.
    // 1- create a job and bind with class which is implementing Job
    // interface.
    JobDetail uploadVerifyJob =
        JobBuilder.newJob(UploadLookUpScheduler.class)
            .requestRecovery(true)
            .withDescription("Scheduler for bulk upload retry")
            .withIdentity("uploadVerifyScheduler", identifier)
            .build();

    // 2- Create a trigger object that will define frequency of run.
    // This will run every day 4:30 AM and in UTC 11 PM
    Trigger uploadTrigger =
        TriggerBuilder.newTrigger()
            .withIdentity("uploadVerifyTrigger", identifier)
            .withSchedule(
                CronScheduleBuilder.cronSchedule(
                    PropertiesCache.getInstance().getProperty("quartz_upload_timer")))
            .build();
    try {
      if (scheduler.checkExists(uploadVerifyJob.getKey())) {
        scheduler.deleteJob(uploadVerifyJob.getKey());
      }
      scheduler.scheduleJob(uploadVerifyJob, uploadTrigger);
      scheduler.start();
      logger.info(
          "SchedulerManager:scheduleBulkUploadJob: UploadLookUpScheduler schedular started");
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  /**
   * This method will do the Quartz scheduler set up in cluster mode.
   *
   * @return Properties
   * @throws IOException
   */
  public Properties setUpClusterMode() throws IOException {
    Properties configProp = new Properties();
    InputStream in = this.getClass().getClassLoader().getResourceAsStream(FILE);
    String host = System.getenv(JsonKey.SUNBIRD_PG_HOST);
    String port = System.getenv(JsonKey.SUNBIRD_PG_PORT);
    String db = System.getenv(JsonKey.SUNBIRD_PG_DB);
    String username = System.getenv(JsonKey.SUNBIRD_PG_USER);
    String password = System.getenv(JsonKey.SUNBIRD_PG_PASSWORD);
    if (!StringUtils.isBlank(host)
        && !StringUtils.isBlank(port)
        && !StringUtils.isBlank(db)
        && !StringUtils.isBlank(username)
        && !StringUtils.isBlank(password)) {
      logger.info("Taking Postgres value from Environment variable...");
      configProp.load(in);
      configProp.put(
          "org.quartz.dataSource.MySqlDS.URL", "jdbc:postgresql://" + host + ":" + port + "/" + db);
      configProp.put("org.quartz.dataSource.MySqlDS.user", username);
      configProp.put("org.quartz.dataSource.MySqlDS.password", password);
      configProp.put("org.quartz.scheduler.instanceName", "MyScheduler");
      logger.info(
          "SchedulerManager:setUpClusterMode: Connection is established from environment variable");
    } else {
      logger.info(
          "SchedulerManager:setUpClusterMode: Environment variable is not set for postgres SQl.");
      configProp = null;
    }
    return configProp;
  }

  public static SchedulerManager getInstance() {
    if (schedulerManager != null) {
      return schedulerManager;
    } else {
      schedulerManager = new SchedulerManager();
    }
    return schedulerManager;
  }

  /**
   * This class will be called by registerShutDownHook to register the call inside jvm , when jvm
   * terminate it will call the run method to clean up the resource.
   *
   * @author Manzarul
   */
  static class ResourceCleanUp extends Thread {
    @Override
    public void run() {
      try {
        scheduler.shutdown();
      } catch (SchedulerException e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  /** Register the hook for resource clean up. this will be called when jvm shut down. */
  public static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
  }

  private void scheduleShadowUser(String identifier) {
    logger.info("SchedulerManager:scheduleShadowUser:scheduleShadowUser scheduler started");
    logger.info(
        "SchedulerManager:scheduleShadowUser:scheduleShadowUser scheduler started second log");
    JobDetail migrateShadowUserJob =
        JobBuilder.newJob(ShadowUserMigrationScheduler.class)
            .requestRecovery(true)
            .withDescription("Scheduler for migrating shadow user ")
            .withIdentity("migrateShadowUserScheduler", identifier)
            .build();
    String shadowUserTime =
        PropertiesCache.getInstance().getProperty("quartz_shadow_user_migration_timer");
    logger.info("SchedulerManager:scheduleShadowUser: schedule time is : " + shadowUserTime);
    Trigger migrateShadowUserTrigger =
        TriggerBuilder.newTrigger()
            .withIdentity("migrateShadowUserTrigger", identifier)
            .withSchedule(CronScheduleBuilder.cronSchedule(shadowUserTime))
            .build();
    try {
      if (scheduler.checkExists(migrateShadowUserJob.getKey())) {
        scheduler.deleteJob(migrateShadowUserJob.getKey());
      }
      scheduler.scheduleJob(migrateShadowUserJob, migrateShadowUserTrigger);
      scheduler.start();
      logger.info("SchedulerManager:scheduleShadowUser:scheduleShadowUser scheduler ended");
    } catch (Exception e) {
      logger.error("SchedulerManager:scheduleShadowUser Error occurred " + e.getMessage(), e);
    }
  }
}
