package org.sunbird.actorutil.systemsettings.impl;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.systemsetting.SystemSetting;

public class SystemSettingClientImpl implements SystemSettingClient {

  private static InterServiceCommunication interServiceCommunication =
      InterServiceCommunicationFactory.getInstance();
  private static SystemSettingClient systemSettingClient = null;
  public static SystemSettingClient getInstance() {
    if (null == systemSettingClient) {
      systemSettingClient = new SystemSettingClientImpl();
    }
    return systemSettingClient;
  }

  @Override
  public SystemSetting getSystemSettingByField(ActorRef actorRef, String field) {
    ProjectLogger.log("SystemSettingClientImpl:getSystemSettingByField: field is " + field, LoggerEnum.INFO.name());
    SystemSetting systemSetting = getSystemSetting(actorRef, JsonKey.FIELD, field);
    return systemSetting;
  }

  @Override
  public <T> T getSystemSettingByFieldAndKey(
      ActorRef actorRef, String field, String key, TypeReference typeReference) {
    SystemSetting systemSetting = getSystemSettingByField(actorRef, field);
    ObjectMapper objectMapper = new ObjectMapper();
    if (systemSetting != null) {
      try {
        Map<String, Object> valueMap = objectMapper.readValue(systemSetting.getValue(), Map.class);
        String[] keys = key.split("\\.");
        int numKeys = keys.length;
        for (int i = 0; i < numKeys - 1; i++) {
          valueMap = objectMapper.convertValue(valueMap.get(keys[i]), Map.class);
        }
        return (T)objectMapper.convertValue(valueMap.get(keys[numKeys - 1]), typeReference);
      } catch (Exception e) {
        ProjectLogger.log(
            "SystemSettingClientImpl:getSystemSettingByFieldAndKey: Exception occurred with error message = "
                + e.getMessage(),
            LoggerEnum.ERROR.name());
      }
    }
    return null;
  }

  private SystemSetting getSystemSetting(ActorRef actorRef, String param, Object value) {
    ProjectLogger.log("SystemSettingClientImpl: getSystemSetting called", LoggerEnum.DEBUG);
    Request request = new Request();
    Map<String, Object> map = new HashMap<>();
    map.put(param, value);
    request.setContext(map);
    request.setOperation(ActorOperations.GET_SYSTEM_SETTING.getValue());
    Object obj = interServiceCommunication.getResponse(actorRef, request);

    if (obj instanceof Response) {
      Response responseObj = (Response) obj;
      return (SystemSetting) responseObj.getResult().get(JsonKey.RESPONSE);
    } else if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else {
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
