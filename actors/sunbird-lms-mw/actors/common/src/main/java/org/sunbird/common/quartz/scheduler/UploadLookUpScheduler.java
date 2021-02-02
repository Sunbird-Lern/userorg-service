/** */
package org.sunbird.common.quartz.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.Util;

/**
 * This class will lookup into bulk process table. if process type is new or in progress (more than
 * x hours) then take the process id and do the re-process of job.
 *
 * @author Manzarul
 */
public class UploadLookUpScheduler extends BaseJob {
  private static LoggerUtil logger = new LoggerUtil(UploadLookUpScheduler.class);

  private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSZ");

  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    logger.info(
        "Running Upload Scheduler Job at: "
            + Calendar.getInstance().getTime()
            + " triggered by: "
            + ctx.getJobDetail().toString());
    Util.DbInfo bulkDb = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
    List<Map<String, Object>> result = null;
    // get List of process with status as New
    Response res = new Response(); // Will depricate these api with SC-2169
    /*cassandraOperation.getRecordsByIndexedProperty(
    bulkDb.getKeySpace(),
    bulkDb.getTableName(),
    JsonKey.STATUS,
    ProjectUtil.BulkProcessStatus.NEW.getValue(),
    null);*/
    result = ((List<Map<String, Object>>) res.get(JsonKey.RESPONSE));
    logger.info(
        "Total No. of record in Bulk_upload_process table with status as NEW are : :"
            + result.size());
    if (!result.isEmpty()) {
      process(result);
    }
    // get List of Process with status as InProgress
    res = new Response(); // Will depricate these api with SC-2169
    /*cassandraOperation.getRecordsByIndexedProperty(
    bulkDb.getKeySpace(),
    bulkDb.getTableName(),
    JsonKey.STATUS,
    ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue(),
    null);*/
    result = ((List<Map<String, Object>>) res.get(JsonKey.RESPONSE));
    logger.info(
        "Total No. of record in Bulk_upload_process table with status as IN_PROGRESS are : :"
            + result.size());
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
          logger.error("UploadLookUpScheduler: " + ex.getMessage(), ex);
        }
      }
      if (!result.isEmpty()) {
        logger.info(
            "Total No. of record in Bulk_upload_process table with status as IN_PROGRESS "
                + "with diff bw start time and current time greater than 5Hr are : :"
                + result.size());
        process(result);
      }
    }
  }

  private void process(List<Map<String, Object>> result) {
    Request request = new Request();
    request.put(JsonKey.DATA, result);
    request.setOperation(ActorOperations.SCHEDULE_BULK_UPLOAD.getValue());
    tellToBGRouter(request);
  }
}
