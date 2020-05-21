package org.sunbird.learner.actors.tac;

import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
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

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    ExecutionContext.setRequestId(request.getRequestId());

    if (operation.equalsIgnoreCase(ActorOperations.USER_TNC_ACCEPT.getValue())) {
      acceptTNC(request);
    } else {
      onReceiveUnsupportedOperation("UserTnCActor");
    }
  }

  private void acceptTNC(Request request) {
    String acceptedTnC = (String) request.getRequest().get(JsonKey.VERSION);
    Map<String, Object> userMap = new HashMap();
    String userId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    SystemSettingClient systemSettingClient = SystemSettingClientImpl.getInstance();
    String latestTnC =
        systemSettingClient.getSystemSettingByFieldAndKey(
            getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
            JsonKey.TNC_CONFIG,
            JsonKey.LATEST_VERSION,
            new TypeReference<String>() {});
    if (!acceptedTnC.equalsIgnoreCase(latestTnC)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), acceptedTnC, JsonKey.VERSION));
    }
    // Search user account in ES
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (result == null || result.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }

    // Check whether user account is locked or not
    if (ProjectUtil.isNotNull(result)
        && result.containsKey(JsonKey.IS_DELETED)
        && ProjectUtil.isNotNull(result.get(JsonKey.IS_DELETED))
        && (Boolean) result.get(JsonKey.IS_DELETED)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
    }

    String lastAcceptedVersion = (String) result.get(JsonKey.TNC_ACCEPTED_VERSION);
    Response response = new Response();
    if (StringUtils.isEmpty(lastAcceptedVersion)
        || !lastAcceptedVersion.equalsIgnoreCase(acceptedTnC)
        || StringUtils.isEmpty((String) result.get(JsonKey.TNC_ACCEPTED_ON))) {
      userMap.put(JsonKey.ID, userId);
      userMap.put(JsonKey.TNC_ACCEPTED_VERSION, acceptedTnC);
      userMap.put(
          JsonKey.TNC_ACCEPTED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
      response =
          cassandraOperation.updateRecord(
              usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userMap);
      if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
        syncUserDetails(userMap);
      }
      sender().tell(response, self());
      generateTelemetry(userMap, acceptedTnC, lastAcceptedVersion);
    } else {
      response.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
    }
  }

  private void generateTelemetry(
      Map<String, Object> userMap, String acceptedTnCVersion, String lastAcceptedVersion) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.USER_ID),
            JsonKey.USER,
            JsonKey.UPDATE,
            lastAcceptedVersion);
    TelemetryUtil.telemetryProcessingCall(userMap, targetObject, correlatedObject);
    ProjectLogger.log(
        "UserTnCActor:syncUserDetails: Telemetry generation call ended ", LoggerEnum.INFO.name());
  }

  private void syncUserDetails(Map<String, Object> completeUserMap) {
    Request userRequest = new Request();
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, completeUserMap.get(JsonKey.ID));
    ProjectLogger.log(
        "UserTnCActor:syncUserDetails: Trigger sync of user details to ES", LoggerEnum.INFO.name());
    tellToAnother(userRequest);
  }
}
