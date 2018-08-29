package controllers.systemsettings;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.validator.systemsettings.SystemSettingsRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class SystemSettingsController extends BaseController {

  private static final SystemSettingsRequestValidator systemSettingsRequestValidator =
      new SystemSettingsRequestValidator();

  /**
   * @param settingId id of the setting to be read
   * @return returns the response result contains the SystemSetting
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> getSystemSettingById(String settingId) {
    try {
      ProjectLogger.log(
          "SystemSettingsController: getSystemSettingById called", LoggerEnum.DEBUG.name());
      Request reqObj = createAndInitRequest(ActorOperations.GET_SYSTEM_SETTING.getValue(), null);
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.ID, settingId);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /** @return returns the response result contains the SystemSetting */
  @SuppressWarnings("unchecked")
  public Promise<Result> setSystemSetting() {
    try {
      ProjectLogger.log(
          "SystemSettingsController: updateSystemSetting called", LoggerEnum.DEBUG.name());
      JsonNode requestJson = request().body().asJson();
      Request reqObj =
          createAndInitRequest(ActorOperations.SET_SYSTEM_SETTING.getValue(), requestJson);
      systemSettingsRequestValidator.validateSetSystemSetting(reqObj);
      reqObj.setRequest(reqObj.getRequest());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /** @return returns the response result contains the list of SystemSettings */
  @SuppressWarnings("unchecked")
  public Promise<Result> getAllSystemSettings() {
    try {
      ProjectLogger.log(
          "SystemSettingsController: getAllSystemSettings called", LoggerEnum.DEBUG.name());
      Request reqObj =
          createAndInitRequest(ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue(), null);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
