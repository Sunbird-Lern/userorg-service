package org.sunbird.user.actors;

import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;

@ActorConfig(
  tasks = {"userSearch"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class KeycloakUserLookupActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    searchUser(request);
  }

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
