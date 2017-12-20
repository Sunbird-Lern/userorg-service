package controllers;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;
import org.sunbird.common.models.response.Response;

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
