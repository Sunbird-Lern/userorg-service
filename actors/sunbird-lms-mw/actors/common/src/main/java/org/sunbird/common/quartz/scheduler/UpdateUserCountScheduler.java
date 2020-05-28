package org.sunbird.common.quartz.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryUtil;

/** @author Amit Kumar */
public class UpdateUserCountScheduler extends BaseJob {

  @Override
  public void execute(JobExecutionContext ctx) {
    ProjectLogger.log(
        "UpdateUserCountScheduler:execute: Triggered Update user count Scheduler Job at: "
            + Calendar.getInstance().getTime()
            + " triggered by: "
            + ctx.getJobDetail().toString(),
        LoggerEnum.INFO.name());
    List<Object> locIdList = new ArrayList<>();
    Util.DbInfo geoLocationDbInfo = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
    Response response =
        cassandraOperation.getAllRecords(
            geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName());
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    for (Map<String, Object> map : list) {
      if (null == map.get(JsonKey.USER_COUNT) || 0 == ((int) map.get(JsonKey.USER_COUNT))) {
        locIdList.add(map.get(JsonKey.ID));
      }
    }
    ProjectLogger.log(
        "UpdateUserCountScheduler:execute: size of total locId to processed = " + locIdList.size());
    Request request = new Request();
    request.setOperation(BackgroundOperations.updateUserCountToLocationID.name());
    request.getRequest().put(JsonKey.LOCATION_IDS, locIdList);
    request.getRequest().put(JsonKey.OPERATION, "UpdateUserCountScheduler");
    ProjectLogger.log(
        "UpdateUserCountScheduler:execute: calling BackgroundService actor from scheduler");
    tellToBGRouter(request);
  }
}
