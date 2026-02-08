package org.sunbird.actor.core;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.routing.FromConfig;
import org.reflections.Reflections;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.PropertiesCache;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.request.Request;
import org.sunbird.response.ResponseCode;

/**
 * BaseRouter class extends {@link BaseActor} and provides routing capabilities to multiple actors.
 * It identifies actors through reflections using {@link ActorConfig} annotations and sets up local
 * or remote routers accordingly.
 */
public abstract class BaseRouter extends BaseActor {

  /**
   * Returns the mode of the router (e.g., LOCAL).
   *
   * @return The router mode.
   */
  public abstract String getRouterMode();

  /**
   * Routes the given request to the appropriate actor.
   *
   * @param request The request to route.
   * @throws Throwable If any error occurs during routing.
   */
  public abstract void route(Request request) throws Throwable;

  /**
   * Caches an actor for a specific key.
   *
   * @param key The key to associate with the actor.
   * @param actor The ActorRef to cache.
   */
  protected abstract void cacheActor(String key, ActorRef actor);

  @Override
  public void onReceive(Request request) throws Throwable {
    String senderPath = sender().path().toString();
    if (RouterMode.LOCAL.name().equalsIgnoreCase(getRouterMode())
        && !StringUtils.startsWith(senderPath, "pekko://")) {
      throw new RouterException(
          "Invalid invocation of the router. Processing not possible from: " + senderPath);
    }
    route(request);
  }

  /**
   * Scans for all classes extending BaseActor within the "org.sunbird" package.
   *
   * @return A set of classes extending BaseActor.
   */
  private Set<Class<? extends BaseActor>> getActors() {
    synchronized (BaseRouter.class) {
      Reflections reflections = new Reflections("org.sunbird");
      return reflections.getSubTypesOf(BaseActor.class);
    }
  }

  /**
   * Initializes actors for the given router context.
   *
   * @param context The actor context.
   * @param name The name of the router (e.g., RequestRouter, BackgroundRequestRouter).
   */
  protected void initActors(ActorContext context, String name) {
    Set<Class<? extends BaseActor>> actors = getActors();
    for (Class<? extends BaseActor> actor : actors) {
      ActorConfig routerDetails = actor.getAnnotation(ActorConfig.class);
      if (null != routerDetails) {
        String dispatcher = routerDetails.dispatcher();
        switch (name) {
          case "BackgroundRequestRouter":
            String[] bgOperations = routerDetails.asyncTasks();
            dispatcher = (StringUtils.isNotBlank(dispatcher)) ? dispatcher : "brr-usr-dispatcher";
            createActor(context, actor, bgOperations, dispatcher);
            break;
          case "RequestRouter":
            String[] operations = routerDetails.tasks();
            dispatcher = (StringUtils.isNotBlank(dispatcher)) ? dispatcher : "rr-usr-dispatcher";
            createActor(context, actor, operations, dispatcher);
            break;
          default:
            logger.info( "Router with name '" + name + "' not supported.");
            break;
        }
      }
    }
  }

  /**
   * Creates an actor and routes allowed operations to it.
   *
   * @param context The actor context.
   * @param actor The actor class.
   * @param operations The operations the actor handles.
   * @param dispatcher The dispatcher to use for this actor.
   */
  private void createActor(
      ActorContext context,
      Class<? extends BaseActor> actor,
      String[] operations,
      String dispatcher) {
    if (null != operations && operations.length > 0) {
      Props props;
      if (StringUtils.isNotBlank(dispatcher)) {
        props = Props.create(actor).withDispatcher(dispatcher);
      } else {
        props = Props.create(actor);
      }
      ActorRef actorRef =
          context.actorOf(FromConfig.getInstance().props(props), actor.getSimpleName());
      for (String operation : operations) {
        String parentName = self().path().name();
        cacheActor(getKey(parentName, operation), actorRef);
      }
    }
  }

  /**
   * Generates a unique key for an operation.
   *
   * @param name The parent router name.
   * @param operation The operation name.
   * @return A composite key string.
   */
  protected static String getKey(String name, String operation) {
    return name + ":" + operation;
  }

  /**
   * Retrieves a property value from environment variables or cache.
   *
   * @param key The property key.
   * @return The property value.
   */
  protected static String getPropertyValue(String key) {
    String mode = System.getenv(key);
    if (StringUtils.isBlank(mode)) {
      mode = PropertiesCache.getInstance().getProperty(key);
    }
    return mode;
  }

  @Override
  public void unSupportedMessage() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, ActorRef.noSender());
  }

  @Override
  public void onReceiveException(String callerName, Exception e) {
    logger.error( callerName + ": exception in message processing = " + e.getMessage(), e);
    sender().tell(e, ActorRef.noSender());
  }
}
