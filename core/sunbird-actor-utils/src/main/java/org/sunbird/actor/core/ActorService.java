package org.sunbird.actor.core;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.routing.FromConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This service class provides methods to instantiate the actor system and initialize actors
 * based on their configurations.
 */
public class ActorService {

  private static ActorSystem system;
  private static Config config =
      ConfigFactory.systemEnvironment().withFallback(ConfigFactory.load());
  private static Map<String, ActorRef> actorRefCache = ActorCache.getActorCache();

  private static ActorService instance = null;

  private ActorService() {}

  /**
   * Gets the singleton instance of ActorService.
   *
   * @return The ActorService instance.
   */
  public static ActorService getInstance() {
    if (instance == null) {
      instance = new ActorService();
    }
    return instance;
  }

  /**
   * Instantiates the actor system and initializes actors from the provided class path list.
   *
   * @param actorSystemName The name of the actor system to create.
   * @param actorsClassPathList List of package paths to scan for {@link ActorConfig} annotated actors.
   */
  public void init(String actorSystemName, List<String> actorsClassPathList) {
    getActorSystem(actorSystemName);
    initActors(actorsClassPathList);
  }

  /**
   * Instantiates or retrieves the actor system.
   *
   * @param actorSystemName Name of the actor system.
   * @return The ActorSystem instance.
   */
  private ActorSystem getActorSystem(String actorSystemName) {
    if (null == system) {
      Config conf = config.getConfig(actorSystemName);
      system = ActorSystem.create(actorSystemName, conf);
    }
    return system;
  }

  /**
   * Initializes actors by scanning class paths and registering them in the cache.
   *
   * @param actorsClassPathList List of package paths to scan.
   */
  private void initActors(List<String> actorsClassPathList) {
    Set<Class<?>> actors = getActors(actorsClassPathList);
    for (Class<?> actor : actors) {
      ActorConfig routerDetails = actor.getAnnotation(ActorConfig.class);
      if (null != routerDetails) {
        String[] operations = routerDetails.tasks();
        String dispatcher =
            (StringUtils.isNotBlank(routerDetails.dispatcher()))
                ? routerDetails.dispatcher()
                : "default-dispatcher";
        createActor(actor, operations, dispatcher);
      }
    }
  }

  /**
   * Scans package paths for classes annotated with {@link ActorConfig}.
   *
   * @param actorsClassPathList List of package paths.
   * @return A set of classes found.
   */
  private Set<Class<?>> getActors(List<String> actorsClassPathList) {
    synchronized (ActorService.class) {
      Reflections reflections = null;
      Set<Class<?>> actors = new HashSet<>();
      for (String classpath : actorsClassPathList) {
        reflections = new Reflections(classpath);
        actors.addAll(reflections.getTypesAnnotatedWith(ActorConfig.class));
      }
      return actors;
    }
  }

  /**
   * Creates an actor instance and maps its configured operations to its reference.
   *
   * @param actor The actor class.
   * @param operations Array of operation names handled by this actor.
   * @param dispatcher The dispatcher to use for this actor.
   */
  private void createActor(Class actor, String[] operations, String dispatcher) {
    if (null != operations && operations.length > 0) {
      Props props;
      if (StringUtils.isNotBlank(dispatcher)) {
        props = Props.create(actor).withDispatcher(dispatcher);
      } else {
        props = Props.create(actor);
      }
      ActorRef actorRef =
          system.actorOf(FromConfig.getInstance().props(props), actor.getSimpleName());
      for (String operation : operations) {
        actorRefCache.put(operation, actorRef);
      }
    }
  }
}
