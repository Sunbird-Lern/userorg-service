package org.sunbird.learner.actors.tac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"userTnCAccept"},
  asyncTasks = {}
)
public class UserTnCActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private ObjectMapper mapper = new ObjectMapper();

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
    Map<String, Object> userMap = new HashMap();
    String userId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    // if managedUserId's terms and conditions are accepted, get userId from request
    String managedUserId = (String) request.getRequest().get(JsonKey.USER_ID);
    boolean isManagedUser = false;
    if (StringUtils.isNotBlank(managedUserId)) {
      userId = managedUserId;
      isManagedUser = true;
    }
    SystemSettingClient systemSettingClient = SystemSettingClientImpl.getInstance();
    String tncType = (String) request.getRequest().get(JsonKey.TNC_TYPE);
    String latestTnC = "";
    // if tncType is null , continue to use the same field for user tnc acceptance
    if (StringUtils.isNotBlank(tncType)) {
      latestTnC =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              tncType,
              JsonKey.LATEST_VERSION,
              new TypeReference<String>() {},
              context);
    } else {
      latestTnC =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              JsonKey.TNC_CONFIG,
              JsonKey.LATEST_VERSION,
              new TypeReference<String>() {},
              context);
    }
    if (!acceptedTnC.equalsIgnoreCase(latestTnC)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), acceptedTnC, JsonKey.VERSION));
    }
    // Search user account in ES
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId, context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (result == null || result.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }

    // If user account isManagedUser(passed in request) and managedBy is empty, not a valid scenario
    if (isManagedUser
        && ProjectUtil.isNotNull(result)
        && ProjectUtil.isNull(result.containsKey(JsonKey.MANAGED_BY))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), userId, JsonKey.USER_ID));
    }

    // Check whether user account is locked or not
    if (ProjectUtil.isNotNull(result)
        && result.containsKey(JsonKey.IS_DELETED)
        && ProjectUtil.isNotNull(result.get(JsonKey.IS_DELETED))
        && (Boolean) result.get(JsonKey.IS_DELETED)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
    }

    String lastAcceptedVersion = "";
    String tncAcceptedOn = "";
    Map<String, Object> allTncAcceptedMap = new HashMap<>();
    if (StringUtils.isBlank(tncType)) {
      lastAcceptedVersion = (String) result.get(JsonKey.TNC_ACCEPTED_VERSION);
      tncAcceptedOn = (String) result.get(JsonKey.TNC_ACCEPTED_ON);
    } else {
      allTncAcceptedMap = (Map<String, Object>) result.get(JsonKey.ALL_TNC_ACCEPTED);
      if (MapUtils.isNotEmpty(allTncAcceptedMap)) {
        Map<String, String> tncAcceptedMap = (Map<String, String>) allTncAcceptedMap.get(tncType);
        if (MapUtils.isNotEmpty(tncAcceptedMap)) {
          lastAcceptedVersion = (String) tncAcceptedMap.get(JsonKey.VERSION);
          tncAcceptedOn = (String) tncAcceptedMap.get(JsonKey.TNC_ACCEPTED_ON);
        }
      } else {
        allTncAcceptedMap = new HashMap<>();
      }
    }

    Response response = new Response();
    if (StringUtils.isEmpty(lastAcceptedVersion)
        || !lastAcceptedVersion.equalsIgnoreCase(acceptedTnC)
        || StringUtils.isEmpty(tncAcceptedOn)) {
      logger.info(
          context,
          "UserTnCActor:acceptTNC: tc accepted version= "
              + acceptedTnC
              + " accepted on= "
              + userMap.get(JsonKey.TNC_ACCEPTED_ON)
              + " for userId:"
              + userId);
      userMap.put(JsonKey.ID, userId);
      if (StringUtils.isBlank(tncType)) {
        userMap.put(JsonKey.TNC_ACCEPTED_VERSION, acceptedTnC);
        userMap.put(
            JsonKey.TNC_ACCEPTED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
      } else {
        Map<String, Object> tncAcceptedMap = new HashMap<>();
        tncAcceptedMap.put(JsonKey.VERSION, acceptedTnC);
        tncAcceptedMap.put(JsonKey.TNC_ACCEPTED_ON, ProjectUtil.getFormattedDate());
        allTncAcceptedMap.put(tncType, tncAcceptedMap);
        userMap.put(JsonKey.ALL_TNC_ACCEPTED, createDBFormatAllTncAccepted(allTncAcceptedMap));
      }
      response =
          cassandraOperation.updateRecord(
              usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userMap, context);
      if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
        if (MapUtils.isNotEmpty(allTncAcceptedMap)) {
          userMap.put(JsonKey.ALL_TNC_ACCEPTED, allTncAcceptedMap);
        }
        syncUserDetails(userMap, context);
      }
      sender().tell(response, self());
      generateTelemetry(userMap, lastAcceptedVersion, request.getContext());
    } else {
      response.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
    }
  }

  private Map<String, Object> createDBFormatAllTncAccepted(Map<String, Object> allTncAcceptedMap) {
    Map<String, Object> allTncMap = new HashMap<>();
    for (Map.Entry<String, Object> mapItr : allTncAcceptedMap.entrySet()) {
      Map<String, Object> tncMap = (Map<String, Object>) mapItr.getValue();
      try {
        allTncMap.put(mapItr.getKey(), mapper.writeValueAsString(tncMap));
      } catch (JsonProcessingException e) {
        logger.error("JsonParsing error while parsing tnc acceptance", e);
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
      }
    }
    return allTncMap;
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

  private void syncUserDetails(Map<String, Object> completeUserMap, RequestContext context) {
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, completeUserMap.get(JsonKey.ID));
    logger.info(context, "UserTnCActor:syncUserDetails: Trigger sync of user details to ES");
    tellToAnother(userRequest);
  }
}
