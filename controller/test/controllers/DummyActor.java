package controllers;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.sunbird.response.Response;

/** Created by arvind on 30/11/17. */
public class DummyActor extends UntypedAbstractActor {

  @Override
  public void onReceive(Object message) throws Throwable {
    Response response = new Response();
    sender().tell(response, ActorRef.noSender());
  }
}
