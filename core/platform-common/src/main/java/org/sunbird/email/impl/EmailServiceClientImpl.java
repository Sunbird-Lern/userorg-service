package org.sunbird.email.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.sunbird.email.EmailServiceClient;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
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
    Object obj = null;
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      obj = Await.result(future, t.duration());
    } catch (ProjectCommonException pce) {
      throw pce;
    } catch (Exception e) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    if (obj instanceof Response) {
      response = (Response) obj;
    } else if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return response;
  }
}
