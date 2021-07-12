package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;

/** This actor process the request for reset password. */
@ActorConfig(
  tasks = {"resetPassword"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class ResetPasswordActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    resetPassword(request);
    generateTelemetry(request);
  }

  private void resetPassword(Request request) {
    String userId = (String) request.get(JsonKey.USER_ID);
    String type = (String) request.get(JsonKey.TYPE);
    logger.info(request.getRequestContext(), "ResetPasswordActor:resetPassword: method called.");
    User user = getUserDao().getUserById(userId, request.getRequestContext());
    ObjectMapper mapper = new ObjectMapper();
    if (null != user) {
      user = removeUnUsedIdentity(user, type);
      Map<String, Object> userMap = mapper.convertValue(user, Map.class);
      UserUtility.decryptUserData(userMap);
      userMap.put(JsonKey.USERNAME, userMap.get(JsonKey.USERNAME));
      userMap.put(JsonKey.REDIRECT_URI, Util.getSunbirdLoginUrl());
      String url = Util.getUserRequiredActionLink(userMap, false, request.getRequestContext());
      userMap.put(JsonKey.SET_PASSWORD_LINK, url);
      if ((String) userMap.get(JsonKey.SET_PASSWORD_LINK) != null) {
        logger.info(
            request.getRequestContext(),
            "ResetPasswordActor:resetPassword: link generated for reset password.");
        Response response = new Response();
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
        response.put(JsonKey.LINK, (String) userMap.get(JsonKey.SET_PASSWORD_LINK));
        sender().tell(response, self());
      } else {
        logger.info(
            request.getRequestContext(),
            "ResetPasswordActor:resetPassword: not able to generate reset password link.");
        ProjectCommonException.throwServerErrorException(ResponseCode.internalError);
      }
    } else {
      ProjectCommonException.throwClientErrorException(ResponseCode.userNotFound);
    }
  }

  private void generateTelemetry(Request request) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) request.get(JsonKey.USER_ID), TelemetryEnvKey.USER, JsonKey.UPDATE, null);
    TelemetryUtil.generateCorrelatedObject(
        (String) request.get(JsonKey.USER_ID), TelemetryEnvKey.USER, null, correlatedObject);
    TelemetryUtil.telemetryProcessingCall(
        request.getRequest(), targetObject, correlatedObject, request.getContext());
  }

  private User removeUnUsedIdentity(User user, String type) {
    if (!(JsonKey.EMAIL.equalsIgnoreCase(type))) {
      user.setEmail(null);
      user.setMaskedEmail(null);
    }
    if (!(JsonKey.PHONE.equalsIgnoreCase(type))) {
      user.setPhone(null);
      user.setMaskedPhone(null);
    }
    if (JsonKey.PREV_USED_PHONE.equalsIgnoreCase(type)) {
      user.setPhone(user.getPrevUsedPhone());
    }
    if (JsonKey.PREV_USED_EMAIL.equalsIgnoreCase(type)) {
      user.setEmail(user.getPrevUsedEmail());
    }
    return user;
  }

  private UserDao getUserDao() {
    return UserDaoImpl.getInstance();
  }
}
