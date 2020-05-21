package org.sunbird.actorutil.courseenrollment;

import akka.actor.ActorRef;
import java.util.Map;
import org.sunbird.common.models.response.Response;

public interface CourseEnrollmentClient {
  /**
   * Unenroll user from course.
   *
   * @param actorRef Actor reference
   * @param request Request containing unenroll information
   * @return Response containing unenroll request status
   */
  Response unenroll(ActorRef actorRef, Map<String, Object> request);
}
