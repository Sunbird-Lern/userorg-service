package org.sunbird.actorutil.courseenrollment.impl;

import akka.actor.ActorRef;
import java.util.Map;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.actorutil.courseenrollment.CourseEnrollmentClient;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class CourseEnrollmentClientImpl implements CourseEnrollmentClient {
  private static InterServiceCommunication interServiceCommunication =
      InterServiceCommunicationFactory.getInstance();
  private static CourseEnrollmentClientImpl courseEnrollmentClient = null;

  public static CourseEnrollmentClientImpl getInstance() {
    if (null == courseEnrollmentClient) {
      courseEnrollmentClient = new CourseEnrollmentClientImpl();
    }
    return courseEnrollmentClient;
  }

  @Override
  public Response unenroll(ActorRef actorRef, Map<String, Object> map) {
    Request request = new Request();
    request.setOperation(ActorOperations.UNENROLL_COURSE.getValue());
    request.setRequest(map);
    Object obj = interServiceCommunication.getResponse(actorRef, request);
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
