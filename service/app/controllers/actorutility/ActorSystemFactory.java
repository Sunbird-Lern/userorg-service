package controllers.actorutility;

import controllers.actorutility.impl.LocalActorSystem;
import controllers.actorutility.impl.RemoteActorSystem;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

/**
 * 
 * @author Amit Kumar
 *
 */
public class ActorSystemFactory {

  private static ActorSystem actorSystem = null;

  private ActorSystemFactory() {}

  static {
    PropertiesCache cache = PropertiesCache.getInstance();
    if ("local".equalsIgnoreCase(cache.getProperty("api_actor_provider"))) {
      ProjectLogger.log("Initializing Normal Local Actor System  called from controller");
      if (null == actorSystem) {
        actorSystem = LocalActorSystem.getInstance();
      }
    } else {
      ProjectLogger.log("Initializing Normal Remote Actor System called from controller");
      if (null == actorSystem) {
        actorSystem = RemoteActorSystem.getInstance();
      }
    }
  }

  public static ActorSystem getActorSystem() {
    return actorSystem;
  }
}
