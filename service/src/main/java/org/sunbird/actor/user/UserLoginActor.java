package org.sunbird.actor.user;

import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

public class UserLoginActor extends UserBaseActor {

  @Override
  public void onReceive(Request request) {
    // Always return 200 and in future release will depricate these apis
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }
}
