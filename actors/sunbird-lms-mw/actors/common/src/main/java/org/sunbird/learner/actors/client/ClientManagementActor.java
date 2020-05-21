package org.sunbird.learner.actors.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;

@ActorConfig(
  tasks = {"registerClient", "updateClientKey", "getClientKey"},
  asyncTasks = {}
)
public class ClientManagementActor extends BaseActor {

  private Util.DbInfo clientDbInfo = Util.dbInfoMap.get(JsonKey.CLIENT_INFO_DB);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.MASTER_KEY);
    // set request id fto thread loacl...
    ExecutionContext.setRequestId(request.getRequestId());

    if (request.getOperation().equalsIgnoreCase(ActorOperations.REGISTER_CLIENT.getValue())) {
      registerClient(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_CLIENT_KEY.getValue())) {
      updateClientKey(request);
    } else if (request.getOperation().equalsIgnoreCase(ActorOperations.GET_CLIENT_KEY.getValue())) {
      getClientKey(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  /**
   * Method to register client
   *
   * @param actorMessage
   */
  @SuppressWarnings("unchecked")
  private void registerClient(Request actorMessage) {
    ProjectLogger.log("Register client method call start");
    String clientName = (String) actorMessage.getRequest().get(JsonKey.CLIENT_NAME);

    // object of telemetry event...
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    Response data = getDataFromCassandra(JsonKey.CLIENT_NAME, clientName);
    List<Map<String, Object>> dataList =
        (List<Map<String, Object>>) data.getResult().get(JsonKey.RESPONSE);
    if (!dataList.isEmpty() && dataList.get(0).containsKey(JsonKey.ID)) {
      throw new ProjectCommonException(
          ResponseCode.invalidClientName.getErrorCode(),
          ResponseCode.invalidClientName.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    // check uniqueness of channel , channel is optional ...
    String channel = (String) actorMessage.getRequest().get(JsonKey.CHANNEL);
    if (!StringUtils.isBlank(channel)) {
      data = getDataFromCassandra(JsonKey.CHANNEL, channel);
      dataList = (List<Map<String, Object>>) data.getResult().get(JsonKey.RESPONSE);
      if (!dataList.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.channelUniquenessInvalid.getErrorCode(),
            ResponseCode.channelUniquenessInvalid.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }

    Map<String, Object> req = new HashMap<>();
    String uniqueId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
    req.put(JsonKey.CLIENT_NAME, StringUtils.remove(clientName.toLowerCase(), " "));
    req.put(JsonKey.ID, uniqueId);
    String masterKey = ProjectUtil.createAuthToken(clientName, uniqueId);
    req.put(JsonKey.MASTER_KEY, masterKey);
    req.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    req.put(JsonKey.CHANNEL, channel);
    Response result =
        cassandraOperation.insertRecord(
            clientDbInfo.getKeySpace(), clientDbInfo.getTableName(), req);
    ProjectLogger.log("Client data saved into cassandra.");
    result.getResult().put(JsonKey.CLIENT_ID, uniqueId);
    result.getResult().put(JsonKey.MASTER_KEY, masterKey);
    result.getResult().remove(JsonKey.RESPONSE);
    // telemetry related data
    targetObject =
        TelemetryUtil.generateTargetObject(uniqueId, JsonKey.MASTER_KEY, JsonKey.CREATE, null);
    TelemetryUtil.generateCorrelatedObject(channel, "channel", "client.channel", correlatedObject);

    sender().tell(result, self());
    TelemetryUtil.telemetryProcessingCall(
        actorMessage.getRequest(), targetObject, correlatedObject);
  }

  /**
   * Method to update client's master key based on client id and master key
   *
   * @param actorMessage
   */
  @SuppressWarnings("unchecked")
  private void updateClientKey(Request actorMessage) {
    ProjectLogger.log("Update client key method call start");
    String clientId = (String) actorMessage.getRequest().get(JsonKey.CLIENT_ID);
    String masterKey = (String) actorMessage.getRequest().get(JsonKey.MASTER_KEY);

    Map<String, Object> targetObject = null;
    // correlated object of telemetry event...
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    // telemetry related data
    targetObject =
        TelemetryUtil.generateTargetObject(clientId, JsonKey.MASTER_KEY, JsonKey.UPDATE, null);

    Response data = getDataFromCassandra(JsonKey.ID, clientId);
    List<Map<String, Object>> dataList =
        (List<Map<String, Object>>) data.getResult().get(JsonKey.RESPONSE);
    if (dataList.isEmpty()
        || !StringUtils.equalsIgnoreCase(
            masterKey, (String) dataList.get(0).get(JsonKey.MASTER_KEY))) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    // check uniqueness of channel , channel is optional ...
    String channel = (String) actorMessage.getRequest().get(JsonKey.CHANNEL);
    if (!StringUtils.isBlank(channel)
        && !channel.equalsIgnoreCase((String) dataList.get(0).get(JsonKey.CHANNEL))) {
      data = getDataFromCassandra(JsonKey.CHANNEL, channel);
      List<Map<String, Object>> dataList1 =
          (List<Map<String, Object>>) data.getResult().get(JsonKey.RESPONSE);
      if (!dataList1.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.channelUniquenessInvalid.getErrorCode(),
            ResponseCode.channelUniquenessInvalid.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }

    String clientName = (String) actorMessage.getRequest().get(JsonKey.CLIENT_NAME);
    if (!StringUtils.isBlank(clientName)
        && !clientName.equalsIgnoreCase((String) dataList.get(0).get(JsonKey.CLIENT_NAME))) {
      data = getDataFromCassandra(JsonKey.CLIENT_NAME, clientName);
      List<Map<String, Object>> dataList1 =
          (List<Map<String, Object>>) data.getResult().get(JsonKey.RESPONSE);
      if (!dataList1.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.invalidClientName.getErrorCode(),
            ResponseCode.invalidClientName.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }

    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.CLIENT_ID, clientId);
    String newMasterKey =
        ProjectUtil.createAuthToken((String) dataList.get(0).get(JsonKey.CLIENT_NAME), clientId);
    req.put(JsonKey.MASTER_KEY, newMasterKey);
    req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    req.put(JsonKey.ID, clientId);
    if (!StringUtils.isBlank(channel)) {
      req.put(JsonKey.CHANNEL, channel);
    }
    if (!StringUtils.isBlank(clientName)) {
      req.put(JsonKey.CLIENT_NAME, clientName);
    }
    req.remove(JsonKey.CLIENT_ID);
    Response result =
        cassandraOperation.updateRecord(
            clientDbInfo.getKeySpace(), clientDbInfo.getTableName(), req);
    ProjectLogger.log("Client data updated into cassandra.");
    result.getResult().put(JsonKey.CLIENT_ID, clientId);
    result.getResult().put(JsonKey.MASTER_KEY, newMasterKey);
    result.getResult().remove(JsonKey.RESPONSE);
    sender().tell(result, self());
    TelemetryUtil.telemetryProcessingCall(
        actorMessage.getRequest(), targetObject, correlatedObject);
  }

  /**
   * Method to get Client details
   *
   * @param actorMessage
   */
  @SuppressWarnings("unchecked")
  private void getClientKey(Request actorMessage) {
    ProjectLogger.log("Get client key method call start");
    String id = (String) actorMessage.getRequest().get(JsonKey.CLIENT_ID);
    String type = (String) actorMessage.getRequest().get(JsonKey.TYPE);
    Response data = null;
    if (JsonKey.CLIENT_ID.equalsIgnoreCase(type)) {
      data = getDataFromCassandra(JsonKey.ID, id);
      List<Map<String, Object>> dataList =
          (List<Map<String, Object>>) data.getResult().get(JsonKey.RESPONSE);
      if (dataList.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else if (JsonKey.CHANNEL.equalsIgnoreCase(type)) {
      data = getDataFromCassandra(JsonKey.CHANNEL, id);
      List<Map<String, Object>> dataList =
          (List<Map<String, Object>>) data.getResult().get(JsonKey.RESPONSE);
      if (dataList.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    sender().tell(data, self());
  }

  /**
   * Method to get data from cassandra for Client_Info table
   *
   * @param propertyName
   * @param propertyValue
   * @return
   */
  private Response getDataFromCassandra(String propertyName, String propertyValue) {
    ProjectLogger.log("Get data from cassandra method call start");
    Response result = null;
    if (StringUtils.equalsIgnoreCase(JsonKey.CLIENT_NAME, propertyName)) {
      result =
          cassandraOperation.getRecordsByProperty(
              clientDbInfo.getKeySpace(),
              clientDbInfo.getTableName(),
              JsonKey.CLIENT_NAME,
              propertyValue.toLowerCase());
    } else if (StringUtils.equalsIgnoreCase(JsonKey.ID, propertyName)) {
      result =
          cassandraOperation.getRecordsByProperty(
              clientDbInfo.getKeySpace(), clientDbInfo.getTableName(), JsonKey.ID, propertyValue);
    } else if (StringUtils.equalsIgnoreCase(JsonKey.CHANNEL, propertyName)) {
      result =
          cassandraOperation.getRecordsByProperty(
              clientDbInfo.getKeySpace(),
              clientDbInfo.getTableName(),
              JsonKey.CHANNEL,
              propertyValue);
    }
    if (null == result || result.getResult().isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return result;
  }
}
