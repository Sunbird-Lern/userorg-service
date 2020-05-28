package org.sunbird.systemsettings.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
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
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.systemsettings.dao.impl.SystemSettingDaoImpl;

@ActorConfig(
  tasks = {"getSystemSetting", "getAllSystemSettings", "setSystemSetting"},
  asyncTasks = {}
)
public class SystemSettingsActor extends BaseActor {
  private final ObjectMapper mapper = new ObjectMapper();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final SystemSettingDaoImpl systemSettingDaoImpl =
      new SystemSettingDaoImpl(cassandraOperation);

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.SYSTEM_SETTINGS);
    ExecutionContext.setRequestId(request.getRequestId());

    switch (request.getOperation()) {
      case "getSystemSetting":
        getSystemSetting(request);
        break;
      case "getAllSystemSettings":
        getAllSystemSettings();
        break;
      case "setSystemSetting":
        setSystemSetting(request);
        break;
      default:
        onReceiveUnsupportedOperation(request.getOperation());
        break;
    }
  }

  private void getSystemSetting(Request actorMessage) {
    ProjectLogger.log(
        "SystemSettingsActor:getSystemSetting: request is " + actorMessage.getRequest(),
        LoggerEnum.INFO.name());
    SystemSetting setting = null;
    String value =
        DataCacheHandler.getConfigSettings()
            .get((String) actorMessage.getContext().get(JsonKey.FIELD));
    ProjectLogger.log(
        "SystemSettingsActor:getSystemSetting:the value got for field from cache is:" + value,
        LoggerEnum.INFO.name());
    if (value != null) {
      setting =
          new SystemSetting(
              (String) actorMessage.getContext().get(JsonKey.FIELD),
              (String) actorMessage.getContext().get(JsonKey.FIELD),
              value);
    }

    if (setting == null) {
      setting =
          systemSettingDaoImpl.readByField((String) actorMessage.getContext().get(JsonKey.FIELD));
      ProjectLogger.log(
          "SystemSettingsActor:getSystemSetting:the value got for field from db",
          LoggerEnum.INFO.name());
      if (null != setting) {
        DataCacheHandler.getConfigSettings()
            .put((String) actorMessage.getContext().get(JsonKey.FIELD), setting.getValue());
      }
    }
    if (setting == null) {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound.getErrorCode(),
          ResponseCode.resourceNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, setting);
    sender().tell(response, self());
  }

  private void getAllSystemSettings() {
    ProjectLogger.log("SystemSettingsActor: getAllSystemSettings called", LoggerEnum.DEBUG.name());
    List<SystemSetting> allSystemSettings = systemSettingDaoImpl.readAll();
    Response response = new Response();
    response.put(JsonKey.RESPONSE, allSystemSettings);
    sender().tell(response, self());
  }

  private void setSystemSetting(Request actorMessage) {
    ProjectLogger.log("SystemSettingsActor: setSystemSetting called", LoggerEnum.DEBUG.name());

    Map<String, Object> request = actorMessage.getRequest();
    String id = (String) request.get(JsonKey.ID);
    String field = (String) request.get(JsonKey.FIELD);
    if (JsonKey.PHONE_UNIQUE.equalsIgnoreCase(field)
        || JsonKey.EMAIL_UNIQUE.equalsIgnoreCase(field)
        || JsonKey.PHONE_UNIQUE.equalsIgnoreCase(id)
        || JsonKey.EMAIL_UNIQUE.equalsIgnoreCase(id)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorUpdateSettingNotAllowed,
          MessageFormat.format(ResponseCode.errorUpdateSettingNotAllowed.getErrorMessage(), field));
    }

    SystemSetting systemSetting = mapper.convertValue(request, SystemSetting.class);
    Response response = systemSettingDaoImpl.write(systemSetting);
    sender().tell(response, self());
  }
}
