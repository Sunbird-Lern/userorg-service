package controllers.courseenrollment;

import controllers.BaseController;
import controllers.courseenrollment.validator.CourseEnrollmentRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class CourseEnrollmentController extends BaseController {
  /**
   * This method will provide list of enrolled courses for a user. User courses are stored in
   * Cassandra db.
   *
   * @param uid user id for whom we need to collect all the courses.
   * @return Result
   */
  public Promise<Result> getEnrolledCourses(String uid) {
    return handleRequest(ActorOperations.GET_COURSE.getValue(), request().body().asJson());
  }

  /**
   * This method will be called when user will enroll for a new course.
   *
   * @return Result
   */
  public Promise<Result> enrollCourse() {
    return handleRequest(
        ActorOperations.ENROLL_COURSE.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseEnrollmentRequestValidator().validateEnrollCourse((Request) request);
          return null;
        });
  }

  /**
   * This method will be called when user will unenroll for course.
   *
   * @return Result
   */
  public Promise<Result> unenrollCourse() {

    return handleRequest(
        ActorOperations.UNENROLL_COURSE.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseEnrollmentRequestValidator().validateUnenrollCourse((Request) request);
          return null;
        });
  }
}
