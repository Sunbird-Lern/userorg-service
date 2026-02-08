package org.sunbird.actor.service;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.routing.FromConfig;
import org.sunbird.actor.core.RouterMode;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.logging.LoggerUtil;

/**
 * BaseMWService is the base class for Sunbird Middleware Services. It handles the initialization of
 * the Pekko ActorSystem and routers (RequestRouter and BackgroundRequestRouter). It supports both
 * local and remote actor system modes.
 */
public class BaseMWService {

  /** Configuration loaded from system environment and default resources. */
  public static Config config =
      ConfigFactory.systemEnvironment().withFallback(ConfigFactory.load());

  private static String actorMode;

  /** The Pekko ActorSystem instance. */
  protected static ActorSystem system;

  /** Name of the ActorSystem. */
  protected static String name = "SunbirdMWSystem";

  /** Reference to the request router actor. */
  protected static ActorRef requestRouter;

  /** Reference to the background request router actor. */
  protected static ActorRef bgRequestRouter;

  /** Logger utility instance for this class. */
  protected static LoggerUtil logger = new LoggerUtil(BaseMWService.class);

  /**
   * Determines the operation mode (local or remote) based on the configured modes for
   * RequestRouter and BackgroundRequestRouter.
   *
   * @return The operation mode ("local" or "remote").
   */
  protected static String getMode() {
    if (StringUtils.isBlank(actorMode)) {
      List<String> routers =
          Arrays.asList(RequestRouter.getMode(), BackgroundRequestRouter.getMode());
      long localCount =
          routers.stream().filter(mode -> StringUtils.equalsIgnoreCase(mode, "local")).count();
      actorMode = (routers.size() == localCount) ? "local" : "remote";
    }
    return actorMode;
  }

  /**
   * Retrieves the request router. If it's not initialized locally, it tries to get a remote
   * selection.
   *
   * @return The ActorRef or ActorSelection for the request router.
   */
  public static Object getRequestRouter() {
    if (null != requestRouter) {
      return requestRouter;
    } else {
      return getRemoteRouter(RequestRouter.class.getSimpleName());
    }
  }

  /**
   * Retrieves the background request router. If it's not initialized locally, it tries to get a
   * remote selection.
   *
   * @return The ActorRef or ActorSelection for the background request router.
   */
  public static Object getBackgroundRequestRouter() {
    if (null != bgRequestRouter) {
      return bgRequestRouter;
    } else {
      return getRemoteRouter(BackgroundRequestRouter.class.getSimpleName());
    }
  }

  /**
   * Retrieves an ActorSelection for a remote router based on its name.
   *
   * @param router The name of the router.
   * @return The ActorSelection for the remote router.
   */
  public static ActorSelection getRemoteRouter(String router) {
    String path = null;
    if (BackgroundRequestRouter.class.getSimpleName().equals(router)) {
      path = config.getString("sunbird_remote_bg_req_router_path");
      return system.actorSelection(path);
    } else if (RequestRouter.class.getSimpleName().equals(router)) {
      path = config.getString("sunbird_remote_req_router_path");
      return system.actorSelection(path);
    } else {
      return null;
    }
  }

  /**
   * Initializes or retrieves the Pekko ActorSystem.
   *
   * @param host The hostname for remote mode.
   * @param port The port for remote mode.
   * @return The initialized ActorSystem.
   */
  protected static ActorSystem getActorSystem(String host, String port) {
    if (null == system) {
      Config conf;
      if ("remote".equals(getMode())) {
        Config remote = getRemoteConfig(host, port);
        conf = remote.withFallback(config.getConfig(name));
      } else {
        conf = config.getConfig(name);
      }
      logger.info( "BaseMWService: ActorSystem starting with mode: " + getMode());
      system = ActorSystem.create(name, conf);
    }
    return system;
  }

  /**
   * Generates a remote configuration for Pekko.
   *
   * @param host The hostname.
   * @param port The port.
   * @return The Config object for remote connectivity.
   */
  protected static Config getRemoteConfig(String host, String port) {
    List<String> details = new ArrayList<>();
    details.add("org.apache.pekko.actor.provider=org.apache.pekko.remote.RemoteActorRefProvider");
    details.add("pekko.remote.enabled-transports = [\"pekko.remote.classic.netty.tcp\"]");
    if (StringUtils.isNotBlank(host)) {
      details.add("pekko.remote.classic.netty.tcp.hostname=" + host);
    }
    if (StringUtils.isNotBlank(port)) {
      details.add("pekko.remote.classic.netty.tcp.port=" + port);
    }

    return ConfigFactory.parseString(StringUtils.join(details, ","));
  }

  /**
   * Initializes the request and background request routers if they are enabled in the
   * configuration.
   */
  protected static void initRouters() {
    logger.info( "BaseMWService: RequestRouter mode: " + RequestRouter.getMode());
    if (!RouterMode.OFF.name().equalsIgnoreCase(RequestRouter.getMode())) {
      requestRouter =
          system.actorOf(
              FromConfig.getInstance()
                  .props(Props.create(RequestRouter.class).withDispatcher("rr-dispatcher")),
              RequestRouter.class.getSimpleName());
    }
    logger.info("BaseMWService: BackgroundRequestRouter mode: " + BackgroundRequestRouter.getMode());
    if (!RouterMode.OFF.name().equalsIgnoreCase(BackgroundRequestRouter.getMode())) {
      bgRequestRouter =
          system.actorOf(
              FromConfig.getInstance()
                  .props(
                      Props.create(BackgroundRequestRouter.class).withDispatcher("brr-dispatcher")),
              BackgroundRequestRouter.class.getSimpleName());
    }
  }
}
