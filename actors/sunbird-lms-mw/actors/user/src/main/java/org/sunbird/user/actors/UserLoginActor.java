package org.sunbird.user.actors;

import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

@ActorConfig(
  tasks = {"userCurrentLogin"},
  asyncTasks = {}
)
public class UserLoginActor extends UserBaseActor {

  @Override
  public void onReceive(Request request) {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }
}
