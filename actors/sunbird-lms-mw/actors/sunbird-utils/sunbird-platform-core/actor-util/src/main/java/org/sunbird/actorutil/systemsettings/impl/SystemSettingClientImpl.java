package org.sunbird.actorutil.systemsettings.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.systemsetting.SystemSetting;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class SystemSettingClientImpl implements SystemSettingClient {
  private static LoggerUtil logger = new LoggerUtil(SystemSettingClientImpl.class);
  private static SystemSettingClient systemSettingClient = null;

  public static SystemSettingClient getInstance() {
    if (null == systemSettingClient) {
      systemSettingClient = new SystemSettingClientImpl();
    }
    return systemSettingClient;
  }

  @Override
  public SystemSetting getSystemSettingByField(
      ActorRef actorRef, String field, RequestContext context) {
    logger.info(context, "SystemSettingClientImpl:getSystemSettingByField: field is " + field);
    SystemSetting systemSetting = getSystemSetting(actorRef, JsonKey.FIELD, field, context);
    return systemSetting;
  }

  @Override
  public <T> T getSystemSettingByFieldAndKey(
      ActorRef actorRef,
      String field,
      String key,
      TypeReference typeReference,
      RequestContext context) {
    SystemSetting systemSetting = getSystemSettingByField(actorRef, field, context);
    ObjectMapper objectMapper = new ObjectMapper();
    if (systemSetting != null) {
      try {
        Map<String, Object> valueMap = objectMapper.readValue(systemSetting.getValue(), Map.class);
        String[] keys = key.split("\\.");
        int numKeys = keys.length;
        for (int i = 0; i < numKeys - 1; i++) {
          valueMap = objectMapper.convertValue(valueMap.get(keys[i]), Map.class);
        }
        return (T) objectMapper.convertValue(valueMap.get(keys[numKeys - 1]), typeReference);
      } catch (Exception e) {
        logger.error(
            context,
            "SystemSettingClientImpl:getSystemSettingByFieldAndKey: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }
    return null;
  }

  private SystemSetting getSystemSetting(
      ActorRef actorRef, String param, Object value, RequestContext context) {
    ProjectLogger.log("SystemSettingClientImpl: getSystemSetting called", LoggerEnum.DEBUG);
    Request request = new Request();
    request.setRequestContext(context);
    Map<String, Object> map = new HashMap<>();
    map.put(param, value);
    request.setContext(map);
    request.setOperation(ActorOperations.GET_SYSTEM_SETTING.getValue());
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      Object obj = Await.result(future, t.duration());

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
    }catch(Exception e){
      throw new ProjectCommonException(
              ResponseCode.SERVER_ERROR.getErrorCode(),
              ResponseCode.SERVER_ERROR.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
