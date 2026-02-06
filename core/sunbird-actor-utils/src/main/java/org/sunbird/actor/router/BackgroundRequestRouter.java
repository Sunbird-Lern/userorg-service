package org.sunbird.actor.router;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.sunbird.actor.core.BaseRouter;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

/**
 * BackgroundRequestRouter handles the routing of asynchronous (background) requests to the
 * appropriate background actors. It maintains a mapping of background operations to their
 * respective actor references.
 */
public class BackgroundRequestRouter extends BaseRouter {

  private static String mode;
  private static String name;
  private static final Map<String, ActorRef> routingMap = new HashMap<>();

  /**
   * Default constructor that initializes the router mode.
   */
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
    String operation = request.getOperation();
    ActorRef ref = routingMap.get(getKey(self().path().name(), operation));
    if (null != ref) {
      ref.tell(request, self());
    } else {
      logger.info(request.getRequestContext(), "BackgroundRequestRouter: Unsupported operation: " + operation);
      onReceiveUnsupportedOperation(operation);
    }
  }

  @Override
  public String getRouterMode() {
    return getMode();
  }

  /**
   * Retrieves the current router mode from properties.
   *
   * @return The router mode string.
   */
  public static String getMode() {
    if (StringUtils.isBlank(mode)) {
      mode = getPropertyValue(JsonKey.BACKGROUND_ACTOR_PROVIDER);
    }
    return mode;
  }

  /**
   * Retrieves the ActorRef for a specific background operation.
   *
   * @param operation The operation name.
   * @return The ActorRef handling the operation.
   */
  public static ActorRef getActor(String operation) {
    return routingMap.get(getKey(name, operation));
  }
}
