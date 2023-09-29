package org.sunbird.actor.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.kafka.InstructionEventGenerator;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.util.PropertiesCache;

public class UserDeletionBackgroundJobActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    inputKafkaTopic(request);
  }

  private void inputKafkaTopic(Request request) throws Exception {
    Map<String, Object> userDetails = request.getRequest();
    String userId = (String) userDetails.get(JsonKey.USER_ID);
    List<String> roles = (List<String>) userDetails.get(JsonKey.ROLES);

    PropertiesCache propertiesCache = PropertiesCache.getInstance();
    String userDeletionTopic = propertiesCache.getProperty("user-deletion-broadcast-topic");

    Map<String, Object> data = new HashMap<>();
    InstructionEventGenerator.pushInstructionEvent(userDeletionTopic, data);
  }
}
