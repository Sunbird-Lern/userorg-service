package org.sunbird.badge.actors;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.badge.service.BadgingService;
import org.sunbird.badge.service.impl.BadgingFactory;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.Util;

/**
 * BadgeClassActor handles BadgeClass requests.
 *
 * @author B Vinaya Kumar
 */
@ActorConfig(
  tasks = {"createBadgeClass", "getBadgeClass", "searchBadgeClass", "deleteBadgeClass"},
  asyncTasks = {}
)
public class BadgeClassActor extends BaseActor {
  private BadgingService badgingService;

  public BadgeClassActor() {
    this.badgingService = BadgingFactory.getInstance();
  }

  public BadgeClassActor(BadgingService badgingService) {
    this.badgingService = badgingService;
  }

  @Override
  public void onReceive(Request request) throws Throwable {
    ProjectLogger.log("BadgeClassActor onReceive called");
    String operation = request.getOperation();

    Util.initializeContext(request, TelemetryEnvKey.BADGE_CLASS);
    ExecutionContext.setRequestId(request.getRequestId());

    switch (operation) {
      case "createBadgeClass":
        createBadgeClass(request);
        break;
      case "getBadgeClass":
        getBadgeClass(request);
        break;
      case "searchBadgeClass":
        searchBadgeClass(request);
        break;
      case "deleteBadgeClass":
        deleteBadgeClass(request);
        break;
      default:
        onReceiveUnsupportedOperation("BadgeClassActor");
    }
  }

  /**
   * Creates a new badge class for a particular issuer.
   *
   * @param actorMessage Request message containing following request data: issuerId: The ID of the
   *     Issuer to be owner of the new Badge Class name: The name of the Badge Class description: A
   *     short description of the new Badge Class. image: An image to represent the Badge Class.
   *     criteria: Either a text string or a URL of a remotely hosted page describing the criteria
   *     rootOrgId: Root organisation ID type: Badge class type (user / content) subtype: Badge
   *     class subtype (e.g. award) roles: JSON array of roles (e.g. [
   *     "OFFICIAL_TEXTBOOK_BADGE_ISSUER" ])
   * @return Return a promise for create badge class API result.
   */
  private void createBadgeClass(Request actorMessage) {
    ProjectLogger.log("createBadgeClass called");

    try {
      Response response = badgingService.createBadgeClass(actorMessage);

      sender().tell(response, self());
    } catch (ProjectCommonException e) {
      ProjectLogger.log("createBadgeClass: exception = ", e);

      sender().tell(e, self());
    }
  }

  /**
   * Get details of requsted badge class.
   *
   * @param actorMessage Request message containing following request data: badgeId The ID of the
   *     Badge Class whose details to view
   */
  private void getBadgeClass(Request actorMessage) {
    ProjectLogger.log("getBadgeClass called");

    try {
      Response response =
          badgingService.getBadgeClassDetails(
              (String) actorMessage.getRequest().get(BadgingJsonKey.BADGE_ID));

      sender().tell(response, self());
    } catch (ProjectCommonException e) {
      ProjectLogger.log("getBadgeClass: exception = ", e);

      sender().tell(e, self());
    }
  }

  /**
   * Get list of badge classes for given issuer(s) and matching given context.
   *
   * @param actorMessage Request message containing following request data issuerList: List of
   *     Issuer IDs whose badge classes are to be listed badgeList: List of badge IDs whose badge
   *     classes are to be listed rootOrgId: Root organisation ID type: Badge class type (user /
   *     content) subtype: Badge class subtype (e.g. award) roles: JSON array of roles (e.g. [
   *     "OFFICIAL_TEXTBOOK_BADGE_ISSUER" ])
   */
  private void searchBadgeClass(Request actorMessage) {
    ProjectLogger.log("searchBadgeClass called");

    try {
      Response response = badgingService.searchBadgeClass(actorMessage);

      sender().tell(response, self());
    } catch (ProjectCommonException e) {
      ProjectLogger.log("searchBadgeClass: exception = ", e);

      sender().tell(e, self());
    }
  }

  /**
   * Delete a badge class that has never been issued.
   *
   * @param actorMessage Request message containing following request data: badgeId The ID of the
   *     Badge Class to delete
   */
  private void deleteBadgeClass(Request actorMessage) {
    ProjectLogger.log("deleteBadgeClass called");

    try {
      Response response = badgingService.removeBadgeClass(actorMessage);

      sender().tell(response, self());
    } catch (ProjectCommonException e) {
      ProjectLogger.log("deleteBadgeClass: exception = ", e);

      sender().tell(e, self());
    }
  }
}
