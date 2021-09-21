package org.sunbird.actor.user;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.Util;

public class CheckUserExistActor extends BaseActor {

  private final UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "checkUserExistence":
        checkUserExistence(request);
        break;
      case "checkUserExistenceV2":
        checkUserExistenceV2(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void checkUserExistence(Request request) {
    Response userResponse = checkUserExists(request, true);
    sender().tell(userResponse, self());
  }

  private void checkUserExistenceV2(Request request) {
    Response userResponse = checkUserExists(request, false);
    sender().tell(userResponse, self());
  }

  private Response checkUserExists(Request request, boolean isV1) {
    Response resp = new Response();
    String key = (String) request.get(JsonKey.KEY);
    if (JsonKey.PHONE.equalsIgnoreCase(key)
        || JsonKey.EMAIL.equalsIgnoreCase(key)
        || JsonKey.USERNAME.equalsIgnoreCase(key)) {
      String value = (String) request.get(JsonKey.VALUE);
      String userId =
          userService.getUserIdByUserLookUp(
              key.toLowerCase(), StringUtils.lowerCase(value), request.getRequestContext());
      if (StringUtils.isBlank(userId)) {
        resp.put(JsonKey.EXISTS, false);
        return resp;
      }
      resp.put(JsonKey.EXISTS, true);
      if (!isV1) {
        User user = userService.getUserById(userId, request.getRequestContext());
        resp.put(JsonKey.ID, user.getId());
        String name = user.getFirstName();
        if (StringUtils.isNotEmpty(user.getLastName())) {
          name += " " + user.getLastName();
        }
        resp.put(JsonKey.NAME, name);
      }
    } else {
      Map<String, Object> responseMap = searchUser(request);
      List<Map<String, Object>> respList = (List) responseMap.get(JsonKey.CONTENT);
      long size = respList.size();
      boolean isExists = (size > 0);
      resp.put(JsonKey.EXISTS, isExists);
      if (isExists && !isV1) {
        Map<String, Object> response = respList.get(0);
        resp.put(JsonKey.EXISTS, true);
        resp.put(JsonKey.ID, response.get(JsonKey.USER_ID));
        String name = (String) response.get(JsonKey.FIRST_NAME);
        if (StringUtils.isNotEmpty((String) response.get(JsonKey.LAST_NAME))) {
          name += " " + response.get(JsonKey.LAST_NAME);
        }
        resp.put(JsonKey.NAME, name);
      }
    }
    return resp;
  }

  private Map<String, Object> searchUser(Request request) {
    Map<String, Object> searchMap = new WeakHashMap<>();
    String value = (String) request.get(JsonKey.VALUE);
    EncryptionService encryptionService =
        org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance();
    String encryptedValue =
        encryptionService.encryptData(StringUtils.lowerCase(value), request.getRequestContext());
    searchMap.put((String) request.get(JsonKey.KEY), encryptedValue);
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, searchMap);
    return userService.searchUser(searchDTO, request.getRequestContext());
  }
}
