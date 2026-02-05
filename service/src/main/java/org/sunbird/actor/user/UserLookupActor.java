package org.sunbird.actor.user;

import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;

public class UserLookupActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    searchUser(request);
  }

  @SuppressWarnings("unchecked")
  private void searchUser(Request request) {
    UserService userService = UserServiceImpl.getInstance();
    Map<String, Object> reqMap = request.getRequest();
    List<String> fields = (List<String>) reqMap.get(JsonKey.FIELDS);
    String key = (String) reqMap.get(JsonKey.KEY);
    String value = (String) reqMap.get(JsonKey.VALUE);
    Response response =
        userService.userLookUpByKey(key, value, fields, request.getRequestContext());
    sender().tell(response, self());
  }
}
