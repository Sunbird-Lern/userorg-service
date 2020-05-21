package org.sunbird.badge.actors;

import java.io.IOException;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.badge.service.BadgingService;
import org.sunbird.badge.service.impl.BadgingFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.Util;

/** Created by arvind on 5/3/18. */
@ActorConfig(
  tasks = {"createBadgeIssuer", "getBadgeIssuer", "getAllIssuer", "deleteIssuer"},
  asyncTasks = {}
)
public class BadgeIssuerActor extends BaseActor {

  private BadgingService badgingService;

  public BadgeIssuerActor() {
    this.badgingService = BadgingFactory.getInstance();
  }

  public BadgeIssuerActor(BadgingService badgingService) {
    this.badgingService = badgingService;
  }

  @Override
  public void onReceive(Request request) throws Throwable {
    ProjectLogger.log("BadgeIssuerActor  onReceive called", LoggerEnum.INFO.name());
    String operation = request.getOperation();

    Util.initializeContext(request, TelemetryEnvKey.BADGE);
    ExecutionContext.setRequestId(request.getRequestId());

    switch (operation) {
      case "createBadgeIssuer":
        createBadgeIssuer(request);
        break;
      case "getBadgeIssuer":
        getBadgeIssuer(request);
        break;
      case "getAllIssuer":
        getAllIssuer(request);
        break;
      case "deleteIssuer":
        deleteIssuer(request);
        break;
      default:
        onReceiveUnsupportedOperation("BadgeIssuerActor");
    }
  }

  /**
   * Actor mathod to create the issuer of badge .
   *
   * @param actorMessage
   */
  private void createBadgeIssuer(Request actorMessage) throws IOException {
    Response response = badgingService.createIssuer(actorMessage);
    sender().tell(response, self());
  }

  private void getBadgeIssuer(Request actorMessage) throws IOException {
    Response response = badgingService.getIssuerDetails(actorMessage);
    sender().tell(response, self());
  }

  private void getAllIssuer(Request actorMessage) throws IOException {
    Response response = badgingService.getIssuerList(actorMessage);
    sender().tell(response, self());
  }

  private void deleteIssuer(Request request) throws IOException {
    Response response = badgingService.deleteIssuer(request);
    sender().tell(response, self());
  }
}
