package org.sunbird.learner.actors.tac;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;

@ActorConfig(
  tasks = {"userTnCAccept"},
  asyncTasks = {}
)
public class UserTnCActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase(ActorOperations.USER_TNC_ACCEPT.getValue())) {
      acceptTNC(request);
    } else {
      onReceiveUnsupportedOperation("UserTnCActor");
    }
  }

  private void acceptTNC(Request request) {
    Util.initializeContext(request, JsonKey.USER);
    RequestContext context = request.getRequestContext();
    String acceptedTnC = (String) request.getRequest().get(JsonKey.VERSION);
    String userId = (String) request.getContext().get(JsonKey.REQUESTED_BY);

    // if managedUserId's terms and conditions are accepted, get userId from request
    String managedUserId = (String) request.getRequest().get(JsonKey.USER_ID);
    boolean isManagedUser = false;
    if (StringUtils.isNotBlank(managedUserId)) {
      userId = managedUserId;
      isManagedUser = true;
    }

    String latestTnC =
        getSystemSettingByFieldAndKey(
            JsonKey.TNC_CONFIG, JsonKey.LATEST_VERSION, new TypeReference<String>() {}, context);

    if (!acceptedTnC.equalsIgnoreCase(latestTnC)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), acceptedTnC, JsonKey.VERSION));
    }

    Response userResponse =
        cassandraOperation.getRecordById(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userId, context);
    List<Map<String, Object>> userList =
        (List<Map<String, Object>>) userResponse.get(JsonKey.RESPONSE);
    Map<String, Object> user;
    if (CollectionUtils.isNotEmpty(userList)) {
      user = userList.get(0);
      if (MapUtils.isEmpty(user)) {
        new ProjectCommonException(
            ResponseCode.userNotFound.getErrorCode(),
            ResponseCode.userNotFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      }
      // Check whether user account is locked or not
      if (user.containsKey(JsonKey.IS_DELETED)
          && ProjectUtil.isNotNull(user.get(JsonKey.IS_DELETED))
          && (Boolean) user.get(JsonKey.IS_DELETED)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
      }
      // If user account isManagedUser(passed in request) and managedBy is empty, not a valid
      // scenario
      if (isManagedUser && ProjectUtil.isNull(user.get(JsonKey.MANAGED_BY))) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(), userId, JsonKey.USER_ID));
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }

    String lastAcceptedVersion = (String) user.get(JsonKey.TNC_ACCEPTED_VERSION);
    Response response = new Response();
    if (StringUtils.isEmpty(lastAcceptedVersion)
        || !lastAcceptedVersion.equalsIgnoreCase(acceptedTnC)
        || StringUtils.isEmpty((String) user.get(JsonKey.TNC_ACCEPTED_ON))) {

      Map<String, Object> userMap = new HashMap();
      userMap.put(JsonKey.ID, userId);
      userMap.put(JsonKey.TNC_ACCEPTED_VERSION, acceptedTnC);
      userMap.put(
          JsonKey.TNC_ACCEPTED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));

      logger.info(
          context,
          "UserTnCActor:acceptTNC: tc accepted version= "
              + acceptedTnC
              + " accepted on= "
              + userMap.get(JsonKey.TNC_ACCEPTED_ON)
              + " for userId:"
              + userId);

      response =
          cassandraOperation.updateRecord(
              usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userMap, context);
      if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
        syncUserDetails(userMap, context);
      }
      sender().tell(response, self());
      generateTelemetry(userMap, lastAcceptedVersion, request.getContext());
    } else {
      response.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
    }
  }

  public <T> T getSystemSettingByFieldAndKey(
      String field, String key, TypeReference typeReference, RequestContext context) {
    ObjectMapper objectMapper = new ObjectMapper();
    String value = DataCacheHandler.getConfigSettings().get(field);
    if (value != null) {
      try {
        Map<String, Object> valueMap = objectMapper.readValue(value, Map.class);
        String[] keys = key.split("\\.");
        int numKeys = keys.length;
        for (int i = 0; i < numKeys - 1; i++) {
          valueMap = objectMapper.convertValue(valueMap.get(keys[i]), Map.class);
        }
        return (T) objectMapper.convertValue(valueMap.get(keys[numKeys - 1]), typeReference);
      } catch (Exception e) {
        logger.error(
            context,
            "getSystemSettingByFieldAndKey: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }
    return null;
  }

  private void generateTelemetry(
      Map<String, Object> userMap, String lastAcceptedVersion, Map<String, Object> context) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.USER_ID),
            JsonKey.USER,
            JsonKey.UPDATE,
            lastAcceptedVersion);
    TelemetryUtil.telemetryProcessingCall(userMap, targetObject, correlatedObject, context);
    ProjectLogger.log(
        "UserTnCActor:syncUserDetails: Telemetry generation call ended ", LoggerEnum.INFO.name());
  }

  private void syncUserDetails(Map<String, Object> userMap, RequestContext context) {
    logger.info(context, "UserTnCActor:syncUserDetails: Trigger sync of user details to ES");
    esService.update(
        ProjectUtil.EsType.user.getTypeName(), (String) userMap.get(JsonKey.ID), userMap, context);
  }
}
