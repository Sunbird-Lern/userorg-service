package org.sunbird.actorutil;

import akka.actor.ActorRef;
import org.sunbird.common.request.Request;
import scala.concurrent.Future;

/** Interface for actor to actor communication. */
public interface InterServiceCommunication {

  /**
   * @param actorRef Actor reference
   * @param request Request object
   * @return Response object
   */
  public Object getResponse(ActorRef actorRef, Request request);

  /*
   * @param actorRef Actor reference
   * @param request Request object
   * @return Future for given actor and request operation
   */
  public Future<Object> getFuture(ActorRef actorRef, Request request);
}
