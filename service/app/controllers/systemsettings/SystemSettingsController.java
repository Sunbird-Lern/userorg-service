package controllers.systemsettings;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * SystemSettingsController This class contains controller methods for System Settings requests
 *
 * @author Loganathan Shanmugam
 */
public class SystemSettingsController extends BaseController {
  /**
   * This method will get SystemSettings by id
   * @param settingId  id of the setting to be read
   * @return returns the response result contains the SystemSetting
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> getSystemSettingById(String settingId) {
    try {
        ProjectLogger.log("SystemSettingsController: getSystemSettingById called", LoggerEnum.DEBUG.name());
        Request reqObj =
          createAndInitRequest(ActorOperations.GET_SYSTEM_SETTING_BY_ID.getValue(), null);
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String, Object> settingsData = reqObj.getRequest();
      settingsData.put(JsonKey.ID,settingId);
      innerMap.put(JsonKey.SYSTEM_SETTINGS, settingsData);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

 /**
   * This method will get all the SystemSettings
   * 
   * @return returns the response result contains the list of SystemSettings
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> getAllSystemSettings() {
    try {
   ProjectLogger.log("SystemSettingsController: getAllSystemSettings called", LoggerEnum.DEBUG.name());
    Request reqObj =
      createAndInitRequest(ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue(), null);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
