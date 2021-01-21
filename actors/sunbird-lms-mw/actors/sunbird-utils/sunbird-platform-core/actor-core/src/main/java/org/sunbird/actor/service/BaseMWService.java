package org.sunbird.actor.service;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.FromConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.RouterMode;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

/** @author Mahesh Kumar Gangula */
public class BaseMWService {

  public static Config config =
      ConfigFactory.systemEnvironment().withFallback(ConfigFactory.load());
  private static String actorMode;
  protected static ActorSystem system;
  protected static String name = "SunbirdMWSystem";
  protected static ActorRef requestRouter;
  protected static ActorRef bgRequestRouter;

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

  public static Object getRequestRouter() {
    if (null != requestRouter) return requestRouter;
    else {
      return getRemoteRouter(RequestRouter.class.getSimpleName());
    }
  }

  public static Object getBackgroundRequestRouter() {
    if (null != bgRequestRouter) return bgRequestRouter;
    else {
      return getRemoteRouter(BackgroundRequestRouter.class.getSimpleName());
    }
  }

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

  protected static ActorSystem getActorSystem(String host, String port) {
    if (null == system) {
      Config conf;
      if ("remote".equals(getMode())) {
        Config remote = getRemoteConfig(host, port);
        conf = remote.withFallback(config.getConfig(name));
      } else {
        conf = config.getConfig(name);
      }
      ProjectLogger.log("ActorSystem starting with mode: " + getMode(), LoggerEnum.INFO.name());
      system = ActorSystem.create(name, conf);
    }
    return system;
  }

  protected static Config getRemoteConfig(String host, String port) {
    List<String> details = new ArrayList<String>();
    details.add("akka.actor.provider=akka.remote.RemoteActorRefProvider");
    details.add("akka.remote.enabled-transports = [\"akka.remote.netty.tcp\"]");
    if (StringUtils.isNotBlank(host)) details.add("akka.remote.netty.tcp.hostname=" + host);
    if (StringUtils.isNotBlank(port)) details.add("akka.remote.netty.tcp.port=" + port);

    return ConfigFactory.parseString(StringUtils.join(details, ","));
  }

  protected static void initRouters() {
    ProjectLogger.log("RequestRouter mode: " + RequestRouter.getMode(), LoggerEnum.INFO.name());
    if (!RouterMode.OFF.name().equalsIgnoreCase(RequestRouter.getMode())) {
      requestRouter =
          system.actorOf(
              FromConfig.getInstance()
                  .props(Props.create(RequestRouter.class).withDispatcher("rr-dispatcher")),
              RequestRouter.class.getSimpleName());
    }
    ProjectLogger.log(
        "BackgroundRequestRouter mode: " + BackgroundRequestRouter.getMode(),
        LoggerEnum.INFO.name());
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
