package org.sunbird.middleware;

import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.learner.util.SchedulerManager;
import org.sunbird.learner.util.Util;

/** @author Mahesh Kumar Gangula */
public class Application {

  public static void main(String[] args) {
    SunbirdMWService.init();
    checkCassandraConnection();
  }

  public static void checkCassandraConnection() {
    Util.checkCassandraDbConnections(JsonKey.SUNBIRD);
    Util.checkCassandraDbConnections(JsonKey.SUNBIRD_PLUGIN);
    SchedulerManager.schedule();
    // scheduler should start after few minutes so internally it is sleeping for 4 minute , so
    // putting in seperate thread .
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                org.sunbird.common.quartz.scheduler.SchedulerManager.getInstance();
              }
            })
        .start();
  }
}
