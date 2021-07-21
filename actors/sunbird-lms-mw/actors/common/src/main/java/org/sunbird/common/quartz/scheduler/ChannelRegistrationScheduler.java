package org.sunbird.common.quartz.scheduler;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

public class ChannelRegistrationScheduler extends BaseJob {
  private static LoggerUtil logger = new LoggerUtil(ChannelRegistrationScheduler.class);

  @Override
  public void execute(JobExecutionContext ctx) {
    logger.info(
        "ChannelRegistrationScheduler:execute: Running channel registration Scheduler Job at: "
            + Calendar.getInstance().getTime()
            + " triggered by: "
            + ctx.getJobDetail().toString());
    Request request = new Request();
    request.setOperation(BackgroundOperations.registerChannel.name());
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.SYSTEM_SETTINGS_DB, JsonKey.CHANNEL_REG_STATUS_ID, null);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      Map<String, Object> resultMap = responseList.get(0);
      logger.info(
          "value for CHANNEL_REG_STATUS_ID (003) from SYSTEM_SETTINGS_DB is : "
              + (String) resultMap.get(JsonKey.VALUE));
      if (StringUtils.isBlank((String) resultMap.get(JsonKey.VALUE))
          && !Boolean.parseBoolean((String) resultMap.get(JsonKey.VALUE))) {
        logger.info(
            "calling ChannelRegistrationActor from ChannelRegistrationScheduler execute method.");
        tellToBGRouter(request);
      }
    } else {
      logger.info(
          "calling ChannelRegistrationActor from ChannelRegistrationScheduler execute method, "
              + "entry for CHANNEL_REG_STATUS_ID (003) is null.");
      tellToBGRouter(request);
    }
  }
}
