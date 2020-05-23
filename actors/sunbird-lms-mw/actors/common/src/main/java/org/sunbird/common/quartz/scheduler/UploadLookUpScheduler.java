/** */
package org.sunbird.common.quartz.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryUtil;

/**
 * This class will lookup into bulk process table. if process type is new or in progress (more than
 * x hours) then take the process id and do the re-process of job.
 *
 * @author Manzarul
 */
public class UploadLookUpScheduler extends BaseJob {
  private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSZ");

  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    ProjectLogger.log(
        "Running Upload Scheduler Job at: "
            + Calendar.getInstance().getTime()
            + " triggered by: "
            + ctx.getJobDetail().toString(),
        LoggerEnum.INFO.name());
    Util.initializeContextForSchedulerJob(
        JsonKey.SYSTEM, ctx.getFireInstanceId(), JsonKey.SCHEDULER_JOB);
    Map<String, Object> logInfo =
        genarateLogInfo(JsonKey.SYSTEM, ctx.getJobDetail().getDescription());
    Util.DbInfo bulkDb = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    List<Map<String, Object>> result = null;
    // get List of process with status as New
    Response res =
        cassandraOperation.getRecordsByProperty(
            bulkDb.getKeySpace(),
            bulkDb.getTableName(),
            JsonKey.STATUS,
            ProjectUtil.BulkProcessStatus.NEW.getValue());
    result = ((List<Map<String, Object>>) res.get(JsonKey.RESPONSE));
    ProjectLogger.log(
        "Total No. of record in Bulk_upload_process table with status as NEW are : :"
            + result.size(),
        LoggerEnum.INFO.name());
    if (!result.isEmpty()) {
      process(result);
    }
    // get List of Process with status as InProgress
    res =
        cassandraOperation.getRecordsByProperty(
            bulkDb.getKeySpace(),
            bulkDb.getTableName(),
            JsonKey.STATUS,
            ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
    result = ((List<Map<String, Object>>) res.get(JsonKey.RESPONSE));
    ProjectLogger.log(
        "Total No. of record in Bulk_upload_process table with status as IN_PROGRESS are : :"
            + result.size(),
        LoggerEnum.INFO.name());
    if (null != result) {
      Iterator<Map<String, Object>> itr = result.iterator();
      while (itr.hasNext()) {
        Map<String, Object> map = itr.next();
        try {
          Date startTime = format.parse((String) map.get(JsonKey.PROCESS_START_TIME));
          Date currentTime = format.parse(format.format(new Date()));
          long difference = currentTime.getTime() - startTime.getTime();
          int hourDiff = (int) (difference / (1000 * 3600));
          // if diff is more than 5Hr then only process it.
          if (hourDiff < 5) {
            itr.remove();
          }
        } catch (ParseException ex) {
          ProjectLogger.log(ex.getMessage(), ex);
        }
      }
      if (!result.isEmpty()) {
        ProjectLogger.log(
            "Total No. of record in Bulk_upload_process table with status as IN_PROGRESS "
                + "with diff bw start time and current time greater than 5Hr are : :"
                + result.size(),
            LoggerEnum.INFO.name());
        process(result);
      }
    }
    TelemetryUtil.telemetryProcessingCall(logInfo, null, null, TelemetryEvents.LOG.getName());
  }

  private void process(List<Map<String, Object>> result) {
    Request request = new Request();
    request.put(JsonKey.DATA, result);
    request.setOperation(ActorOperations.SCHEDULE_BULK_UPLOAD.getValue());
    tellToBGRouter(request);
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
