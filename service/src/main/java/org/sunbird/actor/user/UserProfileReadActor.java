package org.sunbird.actor.user;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserProfileReadService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.Util;

public class UserProfileReadActor extends BaseActor {

  private final EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance();
  private final UserService userService = UserServiceImpl.getInstance();
  private final UserProfileReadService profileReadService = new UserProfileReadService();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "getUserProfileV3":
      case "getUserProfileV4":
      case "getUserProfileV5":
        getUserProfileV3(request);
        break;
      case "getUserDetailsByLoginId":
        getUserDetailsByLoginId(request);
        break;
      case "getUserByKey":
        getUserByKey(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void getUserProfileV3(Request actorMessage) {
    Response response = profileReadService.getUserProfileData(actorMessage);
    sender().tell(response, self());
  }

  private void getUserDetailsByLoginId(Request actorMessage) {
    actorMessage.getRequest().put(JsonKey.KEY, JsonKey.LOGIN_ID);
    actorMessage.getRequest().put(JsonKey.VALUE, actorMessage.getRequest().get(JsonKey.LOGIN_ID));
    getUserByKey(actorMessage);
  }

  private void getUserByKey(Request actorMessage) {
    String key = (String) actorMessage.getRequest().get(JsonKey.KEY);
    String value = (String) actorMessage.getRequest().get(JsonKey.VALUE);
    if (JsonKey.LOGIN_ID.equalsIgnoreCase(key)
        || JsonKey.EMAIL.equalsIgnoreCase(key)
        || JsonKey.USERNAME.equalsIgnoreCase(key)) {
      value = value.toLowerCase();
    }
    String userId = null;
    if (JsonKey.PHONE.equalsIgnoreCase(key)
        || JsonKey.EMAIL.equalsIgnoreCase(key)
        || JsonKey.USERNAME.equalsIgnoreCase(key)) {
      userId =
          userService.getUserIdByUserLookUp(
              key.toLowerCase(), StringUtils.lowerCase(value), actorMessage.getRequestContext());
    } else {
      String encryptedValue =
          encryptionService.encryptData(value, actorMessage.getRequestContext());
      Map<String, Object> searchMap = new WeakHashMap<>();
      searchMap.put(key, encryptedValue);
      SearchDTO searchDTO = new SearchDTO();
      searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, searchMap);
      Map<String, Object> esResponse =
          userService.searchUser(searchDTO, actorMessage.getRequestContext());
      List<Map<String, Object>> userList =
          (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
      if (CollectionUtils.isNotEmpty(userList)) {
        userId = (String) userList.get(0).get(JsonKey.USER_ID);
      }
    }
    if (StringUtils.isNotBlank(userId)) {
      actorMessage.put(JsonKey.USER_ID, userId);
      actorMessage.setOperation(ActorOperations.GET_USER_PROFILE_V5.getValue());
      actorMessage.getContext().put(JsonKey.PRIVATE, false);
      Response response = profileReadService.getUserProfileData(actorMessage);
      sender().tell(response, self());
    } else {
      ProjectCommonException.throwResourceNotFoundException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(ResponseCode.resourceNotFound.getErrorMessage(), value));
    }
  }
}
