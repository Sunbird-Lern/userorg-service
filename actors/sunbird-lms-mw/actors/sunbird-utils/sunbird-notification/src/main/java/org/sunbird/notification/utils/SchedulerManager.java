package org.sunbird.notification.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;

public class SchedulerManager {
  private static LoggerUtil logger = new LoggerUtil(SchedulerManager.class);

  private static final int TTL =
      Integer.parseInt(ProjectUtil.getConfigValue("learner_in_memory_cache_ttl"));

  /*
   * service ScheduledExecutorService object
   */
  public static ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

  /** all scheduler job will be configure here. */
  public static void schedule() {
    service.scheduleWithFixedDelay(new DataCacheHandler(), 0, TTL, TimeUnit.SECONDS);
    logger.info(
        "SchedulerManager:schedule: Started scheduler job for cache refresh with ttl in sec ="
            + TTL);
  }
}
