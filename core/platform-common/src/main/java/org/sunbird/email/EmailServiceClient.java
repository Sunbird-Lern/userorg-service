package org.sunbird.email;

import akka.actor.ActorRef;
import org.sunbird.response.Response;

import java.util.Map;

public interface EmailServiceClient {
  /**
   * Send mail user from course.
   *
   * @param actorRef Actor reference
   * @param request Request containing email realted information
   * @return Response containing email send status
   */
  Response sendMail(ActorRef actorRef, Map<String, Object> request);
}
