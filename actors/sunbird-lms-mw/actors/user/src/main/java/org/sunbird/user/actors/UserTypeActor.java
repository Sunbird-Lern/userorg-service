package org.sunbird.user.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.models.user.UserType;

@ActorConfig(
  tasks = {"getUserTypes"},
  asyncTasks = {}
)
public class UserTypeActor extends UserBaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "getUserTypes":
        getUserTypes();
        break;
    }
  }

  @SuppressWarnings("unchecked")
  private void getUserTypes() {

    Response response = new Response();
    List<Map<String, String>> userTypeList = getUserTypeList();
    response.getResult().put(JsonKey.RESPONSE, userTypeList);
    sender().tell(response, self());
  }

  private List<Map<String, String>> getUserTypeList() {
    List<Map<String, String>> userTypeList = new ArrayList<>();

    for (UserType userType : UserType.values()) {
      Map<String, String> userTypeMap = new HashMap<>();
      userTypeMap.put(JsonKey.ID, userType.getTypeName());
      userTypeMap.put(JsonKey.NAME, userType.getTypeName());
      userTypeList.add(userTypeMap);
    }
    return userTypeList;
  }
}
