package org.sunbird.learner.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This class will manage execute service thread.
 *
 * @author Manzarul.Haque
 */
public final class ExecutorManager {

  private static final int MAX_EXECUTOR_THREAD = 2;
  /*
   * service ScheduledExecutorService object
   */
  private static ScheduledExecutorService service = null;

  private ExecutorManager() {}

  static {
    service = Executors.newScheduledThreadPool(MAX_EXECUTOR_THREAD);
  }

  /**
   * This method will send executor service object.
   *
   * @return
   */
  public static ScheduledExecutorService getExecutorService() {
    return service;
  }
}
