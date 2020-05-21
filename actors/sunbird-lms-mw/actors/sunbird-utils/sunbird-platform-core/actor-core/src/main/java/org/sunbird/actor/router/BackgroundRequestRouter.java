package org.sunbird.actor.router;

import akka.actor.ActorRef;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseRouter;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

/** @author Mahesh Kumar Gangula */
public class BackgroundRequestRouter extends BaseRouter {

  private static String mode;
  private static String name;
  private static Map<String, ActorRef> routingMap = new HashMap<>();

  public BackgroundRequestRouter() {
    getMode();
  }

  @Override
  public void preStart() throws Exception {
    super.preStart();
    name = self().path().name();
    initActors(getContext(), BackgroundRequestRouter.class.getSimpleName());
  }

  @Override
  protected void cacheActor(String key, ActorRef actor) {
    routingMap.put(key, actor);
  }

  @Override
  public void route(Request request) throws Throwable {
    org.sunbird.common.request.ExecutionContext.setRequestId(request.getRequestId());
    String operation = request.getOperation();
    ActorRef ref = routingMap.get(getKey(self().path().name(), operation));
    if (null != ref) {
      ref.tell(request, self());
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  public String getRouterMode() {
    return getMode();
  }

  public static String getMode() {
    if (StringUtils.isBlank(mode)) {
      mode = getPropertyValue(JsonKey.BACKGROUND_ACTOR_PROVIDER);
    }
    return mode;
  }

  public static ActorRef getActor(String operation) {
    return routingMap.get(getKey(name, operation));
  }
}
