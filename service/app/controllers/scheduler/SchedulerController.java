package controllers.scheduler;

import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

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
