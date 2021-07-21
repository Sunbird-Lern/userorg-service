package org.sunbird.learner.actors;

import static org.sunbird.validator.orgvalidator.BaseOrgRequestValidator.ERROR_CODE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.quartz.scheduler.OnDemandSchedulerManager;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

/** @author Amit Kumar */
@ActorConfig(
  tasks = {"onDemandStartScheduler"},
  asyncTasks = {}
)
public class OnDemandSchedulerActor extends BaseActor {
  private static final String TYPE = "type";

  @Override
  public void onReceive(Request actorMessage) throws Throwable {
    if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.ONDEMAND_START_SCHEDULER.getValue())) {
      startSchedular(actorMessage);
    } else {
      onReceiveUnsupportedOperation(actorMessage.getOperation());
    }
  }

  private void startSchedular(Request actorMessage) {
    Map<String, Object> req = actorMessage.getRequest();
    ArrayList<String> jobTypes = (ArrayList<String>) req.get(TYPE);
    if (jobTypes.size() > 0) {
      String[] jobs = jobTypes.stream().toArray(String[]::new);
      validateJobs(jobs);
      scheduleJob(jobs);
    } else {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ResponseCode.mandatoryParamsMissing.getErrorMessage(),
          ERROR_CODE,
          TYPE);
    }
  }

  private void validateJobs(String[] jobs) {
    List<String> jobsAllowed = new ArrayList<String>();
    jobsAllowed.add("bulkupload");
    jobsAllowed.add("shadowuser");
    for (String job : jobs) {
      if (!jobsAllowed.contains(job)) {
        throw new ProjectCommonException(
            ResponseCode.invalidParameter.getErrorCode(),
            ResponseCode.invalidParameter.getErrorMessage(),
            ERROR_CODE,
            TYPE);
      }
    }
  }

  private void scheduleJob(String[] jobs) {
    Response response = new Response();
    OnDemandSchedulerManager onDemandSchedulerManager = OnDemandSchedulerManager.getInstance();
    new Thread(() -> onDemandSchedulerManager.triggerScheduler(jobs)).start();
    Map result = new HashMap<String, Object>();
    result.put(JsonKey.STATUS, JsonKey.SUCCESS);
    response.put(JsonKey.RESULT, result);
    sender().tell(response, self());
  }
}
