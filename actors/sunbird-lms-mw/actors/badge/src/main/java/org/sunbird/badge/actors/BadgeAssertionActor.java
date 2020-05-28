/** */
package org.sunbird.badge.actors;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.badge.BadgeOperations;
import org.sunbird.badge.service.impl.BadgingFactory;
import org.sunbird.badge.util.BadgeAssertionValidator;
import org.sunbird.badge.util.BadgingUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.Util;

import java.io.IOException;
import java.util.Map;

/** @author Manzarul */
@ActorConfig(
  tasks = {"createBadgeAssertion", "getBadgeAssertion", "getBadgeAssertionList", "revokeBadge"},
  asyncTasks = {}
)
public class BadgeAssertionActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    ProjectLogger.log("BadgeAssertionActor onReceive called", LoggerEnum.INFO.name());
    String operation = request.getOperation();

    Util.initializeContext(request, TelemetryEnvKey.BADGE_ASSERTION);

    switch (operation) {
      case "createBadgeAssertion":
        createAssertion(request);
        break;
      case "getBadgeAssertion":
        getAssertionDetails(request);
        break;
      case "getBadgeAssertionList":
        getAssertionList(request);
        break;
      case "revokeBadge":
        revokeAssertion(request);
        break;
      default:
        onReceiveUnsupportedOperation("BadgeClassActor");
    }
  }

  /**
   * This method will call the badger server to create badge assertion.
   *
   * @param actorMessage Request
   */
  private void createAssertion(Request actorMessage) throws IOException {
    ProjectLogger.log(
        "BadgeAssertionActor:createAssertion: Call started ",
        actorMessage.getRequest(),
        LoggerEnum.INFO.name());
    String recipientId = (String) actorMessage.getRequest().get(BadgingJsonKey.RECIPIENT_ID);
    String objectType = (String) actorMessage.getRequest().get(BadgingJsonKey.RECIPIENT_TYPE);
    String badgeId = (String) actorMessage.getRequest().get(BadgingJsonKey.BADGE_ID);
    BadgeAssertionValidator.validateRootOrg(recipientId, objectType, badgeId);
    Response result = BadgingFactory.getInstance().badgeAssertion(actorMessage);
    ProjectLogger.log(
        "BadgeAssertionActor:createAssertion: Assertion Response : " + result.getResult(),
        LoggerEnum.INFO.name());
    sender().tell(result, self());
    Map<String, Object> map = BadgingUtil.createBadgeNotifierMap(result.getResult());
    Request request = new Request();
    ProjectLogger.log(
        "BadgeAssertionActor:createAssertion: Notifying badge assertion for "
            + objectType
            + " with id: "
            + recipientId,
        actorMessage.getRequest(),
        LoggerEnum.INFO.name());
    map.put(JsonKey.OBJECT_TYPE, objectType);
    map.put(JsonKey.ID, recipientId);
    request.getRequest().putAll(map);
    request.setOperation(BadgeOperations.assignBadgeMessage.name());
    tellToAnother(request);
  }

  /**
   * This method will get single assertion details based on issuerSlug, badgeClassSlug and
   * assertionSlug
   *
   * @param request Request
   */
  private void getAssertionDetails(Request request) throws IOException {
    Response result = BadgingFactory.getInstance().getAssertionDetails(request);
    sender().tell(result, self());
  }

  /**
   * This method will get single assertion details based on issuerSlug, badgeClassSlug and
   * assertionSlug
   *
   * @param request Request
   */
  private void getAssertionList(Request request) throws IOException {
    Response result = BadgingFactory.getInstance().getAssertionList(request);
    sender().tell(result, self());
  }

  /**
   * This method will make a call for revoking the badges.
   *
   * @param request Request
   */
  private void revokeAssertion(Request request) throws IOException {
    Response result = BadgingFactory.getInstance().revokeAssertion(request);
    sender().tell(result, self());
    Map<String, Object> map = BadgingUtil.createRevokeBadgeNotifierMap(request.getRequest());
    Request notificationReq = new Request();
    map.put(JsonKey.OBJECT_TYPE, request.getRequest().get(BadgingJsonKey.RECIPIENT_TYPE));
    map.put(JsonKey.ID, request.getRequest().get(BadgingJsonKey.RECIPIENT_ID));
    notificationReq.getRequest().putAll(map);
    notificationReq.setOperation(BadgeOperations.revokeBadgeMessage.name());
    tellToAnother(notificationReq);
  }
}
