package org.sunbird.actor.bulkupload;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.Constants;
import org.sunbird.dao.bulkupload.BulkUploadProcessDao;
import org.sunbird.dao.bulkupload.BulkUploadProcessTaskDao;
import org.sunbird.dao.bulkupload.impl.BulkUploadProcessDaoImpl;
import org.sunbird.dao.bulkupload.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.BulkUploadJsonKey;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.model.bulkupload.BulkUploadProcessTask;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public abstract class BaseBulkUploadBackgroundJobActor extends BaseBulkUploadActor {

  protected void setSuccessTaskStatus(
      BulkUploadProcessTask task,
      ProjectUtil.BulkProcessStatus status,
      Map<String, Object> row,
      String action)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    row.put(JsonKey.OPERATION, action);
    task.setSuccessResult(mapper.writeValueAsString(row));
    task.setStatus(status.getValue());
  }

  protected void setTaskStatus(
      BulkUploadProcessTask task,
      ProjectUtil.BulkProcessStatus status,
      String failureMessage,
      Map<String, Object> row,
      String action)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    row.put(JsonKey.OPERATION, action);
    if (ProjectUtil.BulkProcessStatus.COMPLETED.getValue() == status.getValue()) {
      task.setSuccessResult(mapper.writeValueAsString(row));
      task.setStatus(status.getValue());
    } else if (ProjectUtil.BulkProcessStatus.FAILED.getValue() == status.getValue()) {
      row.put(JsonKey.ERROR_MSG, failureMessage);
      task.setStatus(status.getValue());
      task.setFailureResult(mapper.writeValueAsString(row));
    }
  }

  public void handleBulkUploadBackground(Request request, Function function) {
    String processId = (String) request.get(JsonKey.PROCESS_ID);
    BulkUploadProcessDao bulkUploadDao = BulkUploadProcessDaoImpl.getInstance();
    String logMessagePrefix =
        MessageFormat.format(
            "BaseBulkUploadBackGroundJobActor:handleBulkUploadBackground:{0}: ", processId);

    logger.info(request.getRequestContext(), logMessagePrefix + "called");

    BulkUploadProcess bulkUploadProcess =
        bulkUploadDao.read(processId, request.getRequestContext());
    if (null == bulkUploadProcess) {
      logger.info(request.getRequestContext(), logMessagePrefix + "Invalid process ID.");
      return;
    }

    int status = bulkUploadProcess.getStatus();
    if (!(ProjectUtil.BulkProcessStatus.COMPLETED.getValue() == status)
        || ProjectUtil.BulkProcessStatus.INTERRUPT.getValue() == status) {
      try {
        function.apply(bulkUploadProcess);
      } catch (Exception e) {
        bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
        bulkUploadProcess.setFailureResult(e.getMessage());
        bulkUploadDao.update(bulkUploadProcess, null);
        logger.error(
            request.getRequestContext(),
            logMessagePrefix + "Exception occurred with error message = " + e.getMessage(),
            e);
      }
    }

    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    Response response = bulkUploadDao.update(bulkUploadProcess, request.getRequestContext());
    sender().tell(response, self());
  }

  public void processBulkUpload(
      BulkUploadProcess bulkUploadProcess, Function function, RequestContext context) {
    BulkUploadProcessTaskDao bulkUploadProcessTaskDao = BulkUploadProcessTaskDaoImpl.getInstance();
    String logMessagePrefix =
        MessageFormat.format(
            "BaseBulkUploadBackGroundJobActor:processBulkUpload:{0}: ", bulkUploadProcess.getId());
    Integer sequence = 0;
    Integer taskCount = bulkUploadProcess.getTaskCount();
    List<Map<String, Object>> successList = new LinkedList<>();
    List<Map<String, Object>> failureList = new LinkedList<>();
    while (sequence < taskCount) {
      Integer nextSequence = sequence + getBatchSize(JsonKey.CASSANDRA_WRITE_BATCH_SIZE);
      Map<String, Object> queryMap = new HashMap<>();
      queryMap.put(JsonKey.PROCESS_ID, bulkUploadProcess.getId());
      Map<String, Object> sequenceRange = new HashMap<>();
      sequenceRange.put(Constants.GT, sequence);
      sequenceRange.put(Constants.LTE, nextSequence);
      queryMap.put(BulkUploadJsonKey.SEQUENCE_ID, sequenceRange);
      List<BulkUploadProcessTask> tasks =
          bulkUploadProcessTaskDao.readByPrimaryKeys(queryMap, context);
      if (tasks == null) {
        logger.info(
            context,
            logMessagePrefix
                + "No bulkUploadProcessTask found for process id: "
                + bulkUploadProcess.getId()
                + " and range "
                + sequence
                + ":"
                + nextSequence);
        sequence = nextSequence;
        continue;
      }
      function.apply(tasks);

      try {
        ObjectMapper mapper = new ObjectMapper();
        for (BulkUploadProcessTask task : tasks) {

          if (task.getStatus().equals(ProjectUtil.BulkProcessStatus.FAILED.getValue())) {
            failureList.add(
                mapper.readValue(
                    task.getFailureResult(), new TypeReference<Map<String, Object>>() {}));
          } else if (task.getStatus().equals(ProjectUtil.BulkProcessStatus.COMPLETED.getValue())) {
            successList.add(
                mapper.readValue(
                    task.getSuccessResult(), new TypeReference<Map<String, Object>>() {}));
          }
        }

      } catch (IOException e) {
        logger.error(
            context,
            logMessagePrefix + "Exception occurred with error message = " + e.getMessage(),
            e);
      }
      performBatchUpdate(tasks, context);
      sequence = nextSequence;
    }
    setCompletionStatus(bulkUploadProcess, successList, failureList, context);
  }

  private void setCompletionStatus(
      BulkUploadProcess bulkUploadProcess,
      List successList,
      List failureList,
      RequestContext context) {
    String logMessagePrefix =
        MessageFormat.format(
            "BaseBulkUploadBackGroundJobActor:processBulkUpload:{0}: ", bulkUploadProcess.getId());
    bulkUploadProcess.setSuccessResult(ProjectUtil.convertMapToJsonString(successList));
    bulkUploadProcess.setFailureResult(ProjectUtil.convertMapToJsonString(failureList));
    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    logger.info(context, logMessagePrefix + "completed");
    BulkUploadProcessDao bulkUploadDao = BulkUploadProcessDaoImpl.getInstance();
    bulkUploadDao.update(bulkUploadProcess, context);
  }

  protected void validateMandatoryFields(
      Map<String, Object> csvColumns, BulkUploadProcessTask task, String[] mandatoryFields)
      throws JsonProcessingException {
    if (mandatoryFields != null) {
      for (String field : mandatoryFields) {
        if (StringUtils.isEmpty((String) csvColumns.get(field))) {
          String errorMessage =
              MessageFormat.format(
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(), new Object[] {field});

          setTaskStatus(
              task, ProjectUtil.BulkProcessStatus.FAILED, errorMessage, csvColumns, JsonKey.CREATE);

          ProjectCommonException.throwClientErrorException(
              ResponseCode.mandatoryParamsMissing, errorMessage);
        }
      }
    }
  }

  public abstract void preProcessResult(Map<String, Object> result);

  public Object actorCall(ActorRef actorRef, Request request, RequestContext context) {
    Object obj = null;
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      obj = Await.result(future, t.duration());
    } catch (ProjectCommonException pce) {
      throw pce;
    } catch (Exception e) {
      logger.error(
          context,
          "Unable to communicate with actor: Exception occurred with error message = "
              + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    checkResponseForException(obj);
    return obj;
  }

  private void checkResponseForException(Object obj) {
    if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else if (obj instanceof Exception) {
      throw new ProjectCommonException(
          ResponseCode.serverError,
          ResponseCode.serverError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
