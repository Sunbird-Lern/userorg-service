package org.sunbird.actor.service;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

/** @author Mahesh Kumar Gangula */
public class SunbirdMWService extends BaseMWService {

  public static void init() {
    String host = System.getenv(JsonKey.MW_SYSTEM_HOST);
    String port = System.getenv(JsonKey.MW_SYSTEM_PORT);
    getActorSystem(host, port);
    initRouters();
  }

  public static void tellToRequestRouter(Request request, ActorRef sender) {
    String operation = request.getOperation();
    ActorRef actor = RequestRouter.getActor(operation);
    if (null == actor) {
      ActorSelection select = getRemoteRouter(RequestRouter.class.getSimpleName());
      select.tell(request, sender);
    } else {
      actor.tell(request, sender);
    }
  }

  public static void tellToBGRouter(Request request, ActorRef sender) {
    String operation = request.getOperation();
    ActorRef actor = BackgroundRequestRouter.getActor(operation);
    if (null == actor) {
      ActorSelection select = getRemoteRouter(BackgroundRequestRouter.class.getSimpleName());
      select.tell(request, sender);
    } else {
      actor.tell(request, sender);
    }
  }
}
