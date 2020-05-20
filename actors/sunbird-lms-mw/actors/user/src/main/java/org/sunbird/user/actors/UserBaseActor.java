package org.sunbird.user.actors;

import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;

public abstract class UserBaseActor extends BaseActor {

  private UserRequestValidator userRequestValidator = new UserRequestValidator();
  private ActorRef systemSettingActorRef;
  private SSOManager ssoManager = SSOServiceFactory.getInstance();
  private UserDao userDao = UserDaoImpl.getInstance();
  private UserService userService = UserServiceImpl.getInstance();

  protected void generateTelemetryEvent(
      Map<String, Object> requestMap, String userId, String objectType) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
    Map<String, Object> telemetryAction = new HashMap<>();

    switch (objectType) {
      case "userLevel":
        telemetryAction.put("AssignRole", "role assigned at user level");
        break;
      case "blockUser":
        telemetryAction.put("BlockUser", "user blocked");
        break;
      case "unblockUser":
        telemetryAction.put("UnblockUser", "user unblocked");
        break;
      case "profileVisibility":
        telemetryAction.put("ProfileVisibility", "profile visibility setting changed");
        break;
      default:
        // Do Nothing
    }

    TelemetryUtil.telemetryProcessingCall(telemetryAction, targetObject, correlatedObject);
  }

  protected ActorRef getSystemSettingActorRef() {
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }

    return systemSettingActorRef;
  }

  protected SSOManager getSSOManager() {
    return ssoManager;
  }

  protected UserRequestValidator getUserRequestValidator() {
    return userRequestValidator;
  }

  protected UserDao getUserDao() {
    return userDao;
  }

  public UserService getUserService() {
    return userService;
  }
}
