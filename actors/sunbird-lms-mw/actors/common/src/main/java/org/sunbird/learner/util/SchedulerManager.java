/** */
package org.sunbird.learner.util;

import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.ProjectUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** @author Manzarul All the scheduler job will be handle by this class. */
public class SchedulerManager {
  private static LoggerUtil logger = new LoggerUtil(SchedulerManager.class);

  private static final int TTL =
      Integer.parseInt(ProjectUtil.getConfigValue("learner_in_memory_cache_ttl"));

  /*
   * service ScheduledExecutorService object
   */
  public static ScheduledExecutorService service = ExecutorManager.getExecutorService();

  /** all scheduler job will be configure here. */
  public static void schedule() {
    service.scheduleWithFixedDelay(new DataCacheHandler(), 0, TTL, TimeUnit.SECONDS);
    logger.info(
        "SchedulerManager:schedule: Started scheduler job for cache refresh with ttl in sec ="
            + TTL);
  }
}
