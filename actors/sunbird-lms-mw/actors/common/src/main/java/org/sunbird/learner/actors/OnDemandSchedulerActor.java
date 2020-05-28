package org.sunbird.learner.actors;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.quartz.scheduler.OnDemandSchedulerManager;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.sunbird.common.request.orgvalidator.BaseOrgRequestValidator.ERROR_CODE;

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
      ProjectLogger.log("UNSUPPORTED OPERATION",  LoggerEnum.ERROR);
    }
  }

 private void startSchedular(Request actorMessage) {
   Map<String, Object> req = actorMessage.getRequest();
   ArrayList<String> jobTypes = (ArrayList<String>) req.get(TYPE);
   if(jobTypes.size() > 0) {
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
   for(String job: jobs) {
    if(!jobsAllowed.contains(job)) {
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
