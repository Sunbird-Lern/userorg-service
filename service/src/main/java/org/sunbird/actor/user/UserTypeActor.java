package org.sunbird.actor.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.FormApiUtil;

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
      userTypeLists =
          FormApiUtil.getUserTypeConfig(
              FormApiUtil.getProfileConfig(JsonKey.DEFAULT_PERSONA, request.getRequestContext()));
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
