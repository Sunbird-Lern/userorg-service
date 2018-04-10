package controllers;

import org.sunbird.common.models.response.Response;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;

/**
 * Created by arvind on 30/11/17.
 */
public class DummyActor extends UntypedAbstractActor {

  @Override
  public void onReceive(Object message) throws Throwable {
    Response response = new Response();
    sender().tell(response , ActorRef.noSender());
  }
}
