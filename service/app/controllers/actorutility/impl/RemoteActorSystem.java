package controllers.actorutility.impl;

import controllers.actorutility.ActorSystem;
import org.sunbird.common.models.util.ActorUtility;

/**
 * 
 * @author Amit Kumar
 *
 */
public class RemoteActorSystem implements ActorSystem {
  private static ActorSystem actorSystem = null;

  private RemoteActorSystem() {}

  public static ActorSystem getInstance() {
    if (null == actorSystem) {
      actorSystem = new RemoteActorSystem();
    }
    return actorSystem;
  }

  @Override
  public Object initializeActorSystem() {
    return ActorUtility.getActorSelection();
  }

}
