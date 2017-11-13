package controllers.actorutility.impl;

import controllers.actorutility.ActorSystem;
import org.sunbird.learner.Application;

/**
 * 
 * @author Amit Kumar
 *
 */
public class LocalActorSystem implements ActorSystem {

  private static ActorSystem actorSystem = null;

  private LocalActorSystem() {}

  public static ActorSystem getInstance() {
    if (null == actorSystem) {
      actorSystem = new LocalActorSystem();
    }
    return actorSystem;
  }

  @Override
  public Object initializeActorSystem() {
    return Application.startLocalActorSystem();
  }

}
