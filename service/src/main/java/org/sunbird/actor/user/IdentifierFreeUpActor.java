package org.sunbird.actor.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.Constants;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.keycloak.SSOServiceFactory;
import org.sunbird.common.ProjectUtil;
import org.sunbird.util.user.UserUtil;

/**
 * this Actor class is being used to free Up used User Identifier for now it only free Up user
 * Email, Phone.
 */
public class IdentifierFreeUpActor extends BaseActor {

  private final UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) {
    String id = (String) request.get(JsonKey.ID);
    List<String> identifiers = (List) request.get(JsonKey.IDENTIFIER);
    RequestContext context = request.getRequestContext();
    freeUpUserIdentifier(id, identifiers, context);
  }

  private Response processUserAttribute(
      Map<String, Object> userDbMap, List<String> identifiers, RequestContext context) {
    String userId = (String) userDbMap.get(JsonKey.ID);
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.ID, userId);
    if (identifiers.contains(JsonKey.EMAIL)) {
      nullifyEmail(userDbMap, userMap);
      logger.debug(
          context,
          String.format(
              "%s:%s:Nullified Email. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userId));
    }
    if (identifiers.contains(JsonKey.PHONE)) {
      nullifyPhone(userDbMap, userMap);
      logger.debug(
          context,
          String.format(
              "%s:%s:Nullified Phone. WITH ID  %s",
              this.getClass().getSimpleName(), "freeUpUserIdentifier", userId));
    }

    Response response = new Response();
    if (userMap.size() > 1) {
      // deactivate user
      userMap.put(JsonKey.IS_DELETED, true);
      userMap.put(JsonKey.STATUS, ProjectUtil.Status.INACTIVE.getValue());
      response = userService.updateUser(userMap, context);
      response.getResult().put(JsonKey.USER, userMap);
      // update user status in keycloak
      SSOServiceFactory.getInstance().deactivateUser(userMap, context);
    } else {
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    }
    return response;
  }

  private void nullifyEmail(Map<String, Object> userDbMap, Map<String, Object> updatedUserMap) {
    if (StringUtils.isNotBlank((String) userDbMap.get(JsonKey.EMAIL))) {
      updatedUserMap.put(JsonKey.PREV_USED_EMAIL, userDbMap.get(JsonKey.EMAIL));
      updatedUserMap.put(JsonKey.EMAIL, null);
      updatedUserMap.put(JsonKey.MASKED_EMAIL, null);
      updatedUserMap.put(JsonKey.FLAGS_VALUE, calculateFlagValue(userDbMap));
    }
  }

  private void nullifyPhone(Map<String, Object> userDbMap, Map<String, Object> updatedUserMap) {
    if (StringUtils.isNotBlank((String) userDbMap.get(JsonKey.PHONE))) {
      updatedUserMap.put(JsonKey.PREV_USED_PHONE, userDbMap.get(JsonKey.PHONE));
      updatedUserMap.put(JsonKey.PHONE, null);
      updatedUserMap.put(JsonKey.MASKED_PHONE, null);
      updatedUserMap.put(JsonKey.COUNTRY_CODE, null);
      updatedUserMap.put(JsonKey.FLAGS_VALUE, calculateFlagValue(userDbMap));
    }
  }

  private int calculateFlagValue(Map<String, Object> userDbMap) {
    int flagsValue = 0;
    if (userDbMap.get(JsonKey.FLAGS_VALUE) != null
        && (int) userDbMap.get(JsonKey.FLAGS_VALUE) >= 4) {
      flagsValue = 4;
    }
    return flagsValue;
  }

  private void freeUpUserIdentifier(String id, List<String> identifiers, RequestContext context) {
    User user = userService.getUserById(id, context);
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> userDbMap = mapper.convertValue(user, Map.class);
    Map<String, Object> userLookUpData = new HashMap<>(userDbMap);
    Response response = processUserAttribute(userDbMap, identifiers, context);
    UserUtil.removeEntryFromUserLookUp(userLookUpData, identifiers, context);
    Map<String, Object> updatedUserMap =
        (Map<String, Object>) response.getResult().remove(JsonKey.USER);
    if (MapUtils.isNotEmpty(updatedUserMap)) {
      userService.updateUserDataToES(id, updatedUserMap, context);
    }
    sender().tell(response, self());
    logger.info(
        context,
        String.format(
            "%s:%s:USER SUCCESSFULLY UPDATED IN CASSANDRA. WITH ID  %s",
            this.getClass().getSimpleName(), "freeUpUserIdentifier", userDbMap.get(JsonKey.ID)));
  }
}
