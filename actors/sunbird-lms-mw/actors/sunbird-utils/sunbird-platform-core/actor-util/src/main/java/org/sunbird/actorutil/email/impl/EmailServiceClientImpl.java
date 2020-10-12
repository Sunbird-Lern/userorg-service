package org.sunbird.actorutil.email.impl;

import akka.actor.ActorRef;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.pattern.Patterns;
import akka.util.Timeout;
import org.sunbird.actorutil.email.EmailServiceClient;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class EmailServiceClientImpl implements EmailServiceClient {

  @Override
  public Response sendMail(ActorRef actorRef, Map<String, Object> requestMap) {
    Request actorRequest = new Request();
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.EMAIL_REQUEST, requestMap);
    actorRequest.setOperation((String) requestMap.get(JsonKey.REQUEST));
    actorRequest.setRequest(request);
    Response response = null;
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      Object obj = Await.result(future, t.duration());

      if (obj instanceof Response) {
        response = (Response) obj;
      } else if (obj instanceof ProjectCommonException) {
        throw (ProjectCommonException) obj;
      } else {
        throw new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                ResponseCode.SERVER_ERROR.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
      }
    } catch (Exception e) {
      ProjectCommonException.throwServerErrorException(
              ResponseCode.unableToCommunicateWithActor,
              ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    return response;
  }
}
