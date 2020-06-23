package org.sunbird.actorutil.email.impl;

import akka.actor.ActorRef;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.actorutil.email.EmailServiceClient;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class EmailServiceClientImpl implements EmailServiceClient {
  private static InterServiceCommunication interServiceCommunication =
      InterServiceCommunicationFactory.getInstance();

  @Override
  public Response sendMail(ActorRef actorRef, Map<String, Object> requestMap) {
    Request actorRequest = new Request();
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.EMAIL_REQUEST, requestMap);
    actorRequest.setOperation((String) requestMap.get(JsonKey.REQUEST));
    actorRequest.setRequest(request);

    Object obj = interServiceCommunication.getResponse(actorRef, actorRequest);
    if (obj instanceof Response) {
      Response response = (Response) obj;
      return response;
    } else if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else {
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
