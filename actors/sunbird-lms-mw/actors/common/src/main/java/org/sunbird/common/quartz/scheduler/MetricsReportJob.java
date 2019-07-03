package org.sunbird.common.quartz.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.ReportTrackingStatus;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryUtil;

/** Created by arvind on 30/8/17. */
public class MetricsReportJob extends BaseJob {

  private Util.DbInfo reportTrackingdbInfo = Util.dbInfoMap.get(JsonKey.REPORT_TRACKING_DB);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private int time = -30;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    ProjectLogger.log("MetricsReportJob:execute: Metrics report job trigerred.");
    Util.initializeContextForSchedulerJob(
        JsonKey.SYSTEM, jobExecutionContext.getFireInstanceId(), JsonKey.SCHEDULER_JOB);
    Map<String, Object> logInfo =
        genarateLogInfo(JsonKey.SYSTEM, jobExecutionContext.getJobDetail().getDescription());
    performReportJob();
    TelemetryUtil.telemetryProcessingCall(logInfo, null, null, TelemetryEvents.LOG.getName());
  }

  private void performReportJob() {

    ObjectMapper mapper = new ObjectMapper();
    SimpleDateFormat simpleDateFormat = ProjectUtil.getDateFormatter();
    simpleDateFormat.setLenient(false);

    Response response =
        cassandraOperation.getRecordsByProperty(
            reportTrackingdbInfo.getKeySpace(),
            reportTrackingdbInfo.getTableName(),
            JsonKey.STATUS,
            ReportTrackingStatus.UPLOADING_FILE.getValue());

    List<Map<String, Object>> dbResult = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (!(dbResult.isEmpty())) {
      Calendar now = Calendar.getInstance();
      now.add(Calendar.MINUTE, time);
      Date thirtyMinutesBefore = now.getTime();
      for (Map<String, Object> map : dbResult) {
        String updatedDate = (String) map.get(JsonKey.UPDATED_DATE);
        try {
          if (thirtyMinutesBefore.compareTo(simpleDateFormat.parse(updatedDate)) >= 0) {
            String jsonString = (String) map.get(JsonKey.DATA);
            // convert that string to List<List<Object>>
            TypeReference<List<List<Object>>> typeReference =
                new TypeReference<List<List<Object>>>() {};
            List<List<Object>> data = mapper.readValue(jsonString, typeReference);
            // assign the back ground task to background job actor ...
            Request backGroundRequest = new Request();
            backGroundRequest.setOperation(ActorOperations.FILE_GENERATION_AND_UPLOAD.getValue());

            Map<String, Object> innerMap = new HashMap<>();
            innerMap.put(JsonKey.REQUEST_ID, map.get(JsonKey.ID));
            innerMap.put(JsonKey.DATA, data);

            backGroundRequest.setRequest(innerMap);
            tellToBGRouter(backGroundRequest);
          }
        } catch (ParseException | IOException e) {
          ProjectLogger.log(e.getMessage(), e);
        }
      }
    }

    response =
        cassandraOperation.getRecordsByProperty(
            reportTrackingdbInfo.getKeySpace(),
            reportTrackingdbInfo.getTableName(),
            JsonKey.STATUS,
            ReportTrackingStatus.SENDING_MAIL.getValue());

    dbResult = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (!(dbResult.isEmpty())) {
      Calendar now = Calendar.getInstance();
      now.add(Calendar.MINUTE, time);
      Date thirtyMinutesBefore = now.getTime();
      for (Map<String, Object> map : dbResult) {
        String updatedDate = (String) map.get(JsonKey.UPDATED_DATE);

        try {
          if (thirtyMinutesBefore.compareTo(simpleDateFormat.parse(updatedDate)) >= 0) {

            Request backGroundRequest = new Request();
            backGroundRequest.setOperation(ActorOperations.SEND_MAIL.getValue());

            Map<String, Object> innerMap = new HashMap<>();
            innerMap.put(JsonKey.REQUEST_ID, map.get(JsonKey.ID));

            backGroundRequest.setRequest(innerMap);
            tellToBGRouter(backGroundRequest);
          }

        } catch (ParseException e) {
          ProjectLogger.log(e.getMessage(), e);
        }
      }
    }
  }

  private Map<String, Object> genarateLogInfo(String logType, String message) {

    Map<String, Object> info = new HashMap<>();
    info.put(JsonKey.LOG_TYPE, logType);
    long startTime = System.currentTimeMillis();
    info.put(JsonKey.START_TIME, startTime);
    info.put(JsonKey.MESSAGE, message);
    info.put(JsonKey.LOG_LEVEL, JsonKey.INFO);

    return info;
  }
}
