/** */
package org.sunbird.learner.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.sunbird.common.models.util.LoggerUtil;

/** @author Manzarul All the scheduler job will be handle by this class. */
public class SchedulerManager {
  private static LoggerUtil logger = new LoggerUtil(SchedulerManager.class);

  private static final int TTL = 4;

  /*
   * service ScheduledExecutorService object
   */
  public static ScheduledExecutorService service = ExecutorManager.getExecutorService();

  /** all scheduler job will be configure here. */
  public static void schedule() {
    service.scheduleWithFixedDelay(new DataCacheHandler(), 0, TTL, TimeUnit.HOURS);
    logger.info("SchedulerManager:schedule: Started scheduler job for cache refresh.");
  }
}
