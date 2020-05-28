package org.sunbird.actor.router;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseRouter;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/** @author Mahesh Kumar Gangula */
public class RequestRouter extends BaseRouter {

  private static String mode;
  private static String name;
  public static Map<String, ActorRef> routingMap = new HashMap<>();

  public RequestRouter() {
    getMode();
  }

  @Override
  public void preStart() throws Exception {
    super.preStart();
    name = self().path().name();
    initActors(getContext(), RequestRouter.class.getSimpleName());
  }

  @Override
  protected void cacheActor(String key, ActorRef actor) {
    routingMap.put(key, actor);
  }

  @Override
  public void route(Request request) throws Throwable {
    org.sunbird.common.request.ExecutionContext.setRequestId(request.getRequestId());
    String operation = request.getOperation();
    ActorRef ref = routingMap.get(getKey(self().path().name(), operation));
    if (null != ref) {
      route(ref, request, getContext().dispatcher());
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  public static ActorRef getActor(String operation) {
    return routingMap.get(getKey(name, operation));
  }

  public String getRouterMode() {
    return getMode();
  }

  public static String getMode() {
    if (StringUtils.isBlank(mode)) {
      mode = getPropertyValue(JsonKey.API_ACTOR_PROVIDER);
    }
    return mode;
  }

  /**
   * method will route the message to corresponding router pass into the argument .
   *
   * @param router
   * @param message
   * @return boolean
   */
  private boolean route(ActorRef router, Request message, ExecutionContext ec) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Actor Service Call start  for  api =="
            + message.getOperation()
            + " start time "
            + startTime,
        LoggerEnum.PERF_LOG);
    Timeout timeout = new Timeout(Duration.create(message.getTimeout(), TimeUnit.SECONDS));
    Future<Object> future = Patterns.ask(router, message, timeout);
    ActorRef parent = sender();
    future.onComplete(
        new OnComplete<Object>() {
          @Override
          public void onComplete(Throwable failure, Object result) {
            if (failure != null) {
              // We got a failure, handle it here
              ProjectLogger.log(failure.getMessage(), failure);
              if (failure instanceof ProjectCommonException) {
                parent.tell(failure, self());
              } else if (failure instanceof akka.pattern.AskTimeoutException) {
                ProjectCommonException exception =
                    new ProjectCommonException(
                        ResponseCode.operationTimeout.getErrorCode(),
                        ResponseCode.operationTimeout.getErrorMessage(),
                        ResponseCode.SERVER_ERROR.getResponseCode());
                parent.tell(exception, self());

              } else {
                ProjectCommonException exception =
                    new ProjectCommonException(
                        ResponseCode.internalError.getErrorCode(),
                        ResponseCode.internalError.getErrorMessage(),
                        ResponseCode.SERVER_ERROR.getResponseCode());
                parent.tell(exception, self());
              }
            } else {
              parent.tell(result, self());
            }
          }
        },
        ec);
    return true;
  }
}
