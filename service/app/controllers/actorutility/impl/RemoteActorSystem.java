package controllers.actorutility.impl;

import controllers.actorutility.ActorSystem;
import org.sunbird.common.models.util.ActorUtility;

/**
 * 
 * @author Amit Kumar
 *
 */
public class RemoteActorSystem implements ActorSystem{

  @Override
  public Object initializeActorSystem() {
    return ActorUtility.getActorSelection();
  }

}
