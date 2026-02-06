package org.sunbird.actor.service;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

/**
 * SunbirdMWService is the concrete implementation of {@link BaseMWService}. It provides entry points
 * for initializing the actor system and communicating with routers.
 */
public class SunbirdMWService extends BaseMWService {

  /** Initializes the ActorSystem and routers using environment variables for host and port. */
  public static void init() {
    String host = System.getenv(JsonKey.MW_SYSTEM_HOST);
    String port = System.getenv(JsonKey.MW_SYSTEM_PORT);
    getActorSystem(host, port);
    initRouters();
  }

  /**
   * Routes a request to the appropriate RequestRouter actor.
   *
   * @param request The request to route.
   * @param sender The sender actor reference.
   */
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

  /**
   * Routes a request to the appropriate BackgroundRequestRouter actor.
   *
   * @param request The request to route.
   * @param sender The sender actor reference.
   */
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
