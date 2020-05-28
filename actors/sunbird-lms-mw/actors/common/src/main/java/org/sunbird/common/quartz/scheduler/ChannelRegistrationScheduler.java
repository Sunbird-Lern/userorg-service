package org.sunbird.common.quartz.scheduler;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
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

public class ChannelRegistrationScheduler extends BaseJob {

  @Override
  public void execute(JobExecutionContext ctx) {
    ProjectLogger.log(
        "ChannelRegistrationScheduler:execute: Running channel registration Scheduler Job at: "
            + Calendar.getInstance().getTime()
            + " triggered by: "
            + ctx.getJobDetail().toString(),
        LoggerEnum.INFO.name());
    Request request = new Request();
    request.setOperation(BackgroundOperations.registerChannel.name());
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.SYSTEM_SETTINGS_DB, JsonKey.CHANNEL_REG_STATUS_ID);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      Map<String, Object> resultMap = responseList.get(0);
      ProjectLogger.log(
          "value for CHANNEL_REG_STATUS_ID (003) from SYSTEM_SETTINGS_DB is : "
              + (String) resultMap.get(JsonKey.VALUE));
      if (StringUtils.isBlank((String) resultMap.get(JsonKey.VALUE))
          && !Boolean.parseBoolean((String) resultMap.get(JsonKey.VALUE))) {
        ProjectLogger.log(
            "calling ChannelRegistrationActor from ChannelRegistrationScheduler execute method.");
        tellToBGRouter(request);
      }
    } else {
      ProjectLogger.log(
          "calling ChannelRegistrationActor from ChannelRegistrationScheduler execute method, "
              + "entry for CHANNEL_REG_STATUS_ID (003) is null.");
      tellToBGRouter(request);
    }
  }
}
