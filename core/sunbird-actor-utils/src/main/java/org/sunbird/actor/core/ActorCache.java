package org.sunbird.actor.core;

import org.apache.pekko.actor.ActorRef;

import java.util.HashMap;
import java.util.Map;

/**
 * This class maintains a cache of actor references mapped to their operations.
 * NOTE: Currently does not handle remote actor references.
 */
public class ActorCache {

  private ActorCache() {}

  private static Map<String, ActorRef> actorRefCache = new HashMap<>();

  /**
   * Retrieves the entire cache of actor operations and their corresponding references.
   *
   * @return A map where keys are operation names and values are ActorRef instances.
   */
  public static Map<String, ActorRef> getActorCache() {
    return actorRefCache;
  }

  /**
   * Retrieves an actor reference based on the specified operation.
   *
   * @param actorOperation The operation performed by the actor.
   * @return The associated ActorRef, or null if not found.
   */
  public static ActorRef getActorRef(String actorOperation) {
    return actorRefCache.get(actorOperation);
  }
}
