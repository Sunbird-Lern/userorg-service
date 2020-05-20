package org.sunbird.actorutil.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class InterServiceCommunicationImpl implements InterServiceCommunication {

  private Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));

  @Override
  public Object getResponse(ActorRef actorRef, Request request) {
    try {
      return Await.result(getFuture(actorRef, request), t.duration());
    } catch (Exception e) {
      ProjectLogger.log(
          "InterServiceCommunicationImpl:getResponse: Exception occurred with error message = "
              + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    return null;
  }

  @Override
  public Future<Object> getFuture(ActorRef actorRef, Request request) {
    if (null == actorRef) {
      ProjectLogger.log(
          "InterServiceCommunicationImpl:getFuture: actorRef is null", LoggerEnum.INFO);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    try {
      return Patterns.ask(actorRef, request, t);
    } catch (Exception e) {
      ProjectLogger.log(
          "InterServiceCommunicationImpl:getFuture: Exception occured with error message = "
              + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    return null;
  }
}
