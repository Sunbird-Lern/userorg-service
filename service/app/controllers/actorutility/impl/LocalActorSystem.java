package controllers.actorutility.impl;

import controllers.actorutility.ActorSystem;
import org.sunbird.learner.Application;

/**
 * 
 * @author Amit Kumar
 *
 */
public class LocalActorSystem implements ActorSystem{

  @Override
  public Object initializeActorSystem() {
    return Application.startLocalActorSystem();
  }

  
 
}
