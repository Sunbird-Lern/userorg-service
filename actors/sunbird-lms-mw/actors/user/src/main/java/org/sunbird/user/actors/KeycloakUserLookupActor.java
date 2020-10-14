package org.sunbird.user.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.util.UserLookUp;

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
    UserDao userDao = UserDaoImpl.getInstance();
    Map<String, Object> reqMap = request.getRequest();
    List<String> fields = (List<String>) reqMap.get(JsonKey.FIELDS);
    String key = (String) reqMap.get(JsonKey.KEY);
    String value = (String) reqMap.get(JsonKey.VALUE);
    Response response;
    if (JsonKey.ID.equalsIgnoreCase(key)) {
      List<String> ids = new ArrayList<>(2);
      ids.add(value);
      response = userDao.getUserPropertiesById(ids, fields, request.getRequestContext());
    } else {
      UserLookUp userLookUp = new UserLookUp();
      List<Map<String, Object>> records =
          userLookUp.getRecordByType(
              key.toLowerCase(), value.toLowerCase(), true, request.getRequestContext());
      List<String> ids = new ArrayList<>();
      records
          .stream()
          .forEach(
              record -> {
                ids.add((String) record.get(JsonKey.USER_ID));
              });
      response = userDao.getUserPropertiesById(ids, fields, request.getRequestContext());
    }
    for (Map<String, Object> userMap :
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE)) {
      UserUtility.decryptUserDataFrmES(userMap);
    }
    sender().tell(response, self());
  }
}
