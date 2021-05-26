package org.sunbird.user.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.FormApiUtil;

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
        getUserTypes(request);
        break;
    }
  }

  @SuppressWarnings("unchecked")
  private void getUserTypes(Request request) {

    Response response = new Response();
    List<Map<String, String>> userTypeList = getUserTypeList(request);
    response.getResult().put(JsonKey.RESPONSE, userTypeList);
    sender().tell(response, self());
  }

  private List<Map<String, String>> getUserTypeList(Request request) {
    List<Map<String, String>> userTypes = new ArrayList<>();

    Map<String, Map<String, List<String>>> userTypeConfigList =
        DataCacheHandler.getUserTypesConfig();
    Map<String, List<String>> userTypeLists = userTypeConfigList.get(JsonKey.DEFAULT_PERSONA);
    if (MapUtils.isEmpty(userTypeLists)) {
      Map<String, Object> userProfileConfigMap =
          FormApiUtil.getProfileConfig(JsonKey.DEFAULT_PERSONA, request.getRequestContext());
      if (MapUtils.isNotEmpty(userProfileConfigMap)) {
        Map<String, Object> formData =
            (Map<String, Object>) userProfileConfigMap.get(JsonKey.DEFAULT_PERSONA);
        userTypeLists = FormApiUtil.getUserTypeConfig(formData);
      }
    }
    for (Map.Entry<String, List<String>> itr : userTypeLists.entrySet()) {
      Map<String, String> userTypeMap = new HashMap<>();
      userTypeMap.put(JsonKey.ID, itr.getKey());
      userTypeMap.put(JsonKey.NAME, itr.getKey());
      userTypes.add(userTypeMap);
    }
    return userTypes;
  }
}
