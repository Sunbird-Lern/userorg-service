package controllers.scheduler;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class SchedulerController extends BaseController {
 public CompletionStage<Result> startScheduler(Http.Request httpRequest) {
  return handleRequest(
          ActorOperations.ONDEMAND_START_SCHEDULER.getValue(),
          httpRequest.body().asJson(),
          null,
          null,
          null,
          true,
          httpRequest);
 }
}
