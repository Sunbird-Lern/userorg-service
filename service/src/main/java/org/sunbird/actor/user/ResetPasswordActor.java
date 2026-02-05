package org.sunbird.actor.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.ResetPasswordService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.keycloak.KeycloakBruteForceAttackUtil;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.UserUtility;
import org.sunbird.util.Util;

public class ResetPasswordActor extends BaseActor {

  private final UserService userService = UserServiceImpl.getInstance();
  private final ResetPasswordService resetPasswordService = new ResetPasswordService();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    resetPassword(request);
    generateTelemetry(request);
  }

  private void resetPassword(Request request) throws Exception {
    String userId = (String) request.get(JsonKey.USER_ID);
    logger.debug(request.getRequestContext(), "ResetPasswordActor:resetPassword: method called.");
    User user = userService.getUserById(userId, request.getRequestContext());
    boolean isDisabled =
        KeycloakBruteForceAttackUtil.isUserAccountDisabled(
            user.getUserId(), request.getRequestContext());
    if (isDisabled) {
      KeycloakBruteForceAttackUtil.unlockTempDisabledUser(
          user.getUserId(), request.getRequestContext());
    }
    generateLink(request, user);
  }

  private void generateLink(Request request, User user) {
    ObjectMapper mapper = new ObjectMapper();
    String type = (String) request.get(JsonKey.TYPE);
    user = removeUnUsedIdentity(user, type);
    Map<String, Object> userMap = mapper.convertValue(user, Map.class);
    UserUtility.decryptUserData(userMap);
    userMap.put(JsonKey.USERNAME, userMap.get(JsonKey.USERNAME));
    userMap.put(JsonKey.REDIRECT_URI, resetPasswordService.getSunbirdLoginUrl());
    String url =
        resetPasswordService.getUserRequiredActionLink(userMap, false, request.getRequestContext());
    userMap.put(JsonKey.SET_PASSWORD_LINK, url);
    if (StringUtils.isNotBlank(url)) {
      logger.debug(
          request.getRequestContext(),
          "ResetPasswordActor:generateLink: link generated for reset password.");
      Response response = new Response();
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      response.put(JsonKey.LINK, url);
      sender().tell(response, self());
    } else {
      logger.debug(
          request.getRequestContext(),
          "ResetPasswordActor:generateLink: not able to generate reset password link.");
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
  }

  private void generateTelemetry(Request request) {
    Map<String, Object> targetObject;
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
}
