package org.sunbird.badge.actors;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.sunbird.badge.BadgeOperations;
import org.sunbird.badge.service.impl.BadgrServiceImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import scala.concurrent.duration.FiniteDuration;

/** Created by arvind on 15/3/18. */
public class BadgeIssuerActorTest {

  private static final FiniteDuration ACTOR_MAX_WAIT_DURATION = duration("100 second");

  private ActorSystem system;
  private Props props;

  private TestKit probe;
  private ActorRef subject;

  private Request actorMessage;

  BadgrServiceImpl mockBadgingService;
  ProjectCommonException resourceNotFoundException;

  @Before
  public void setUp() {
    system = ActorSystem.create("system");
    probe = new TestKit(system);
    mockBadgingService = PowerMockito.mock(BadgrServiceImpl.class);
    props = Props.create(BadgeIssuerActor.class, mockBadgingService);
    subject = system.actorOf(props);
    actorMessage = new Request();
    ResponseCode error = ResponseCode.resourceNotFound;
    resourceNotFoundException =
        new ProjectCommonException(
            error.getErrorCode(), error.getErrorMessage(), error.getResponseCode());
  }

  @Test
  public void testCreateBadgeIssuerSuccess() throws IOException {
    Response response = new Response();

    response.put(JsonKey.RESPONSE, createHttpUtilRes(200, mapToJson(createBadgeIssuerResponse())));

    PowerMockito.when(mockBadgingService.createIssuer(actorMessage)).thenReturn(response);
    actorMessage.setOperation(BadgeOperations.createBadgeIssuer.name());
    subject.tell(actorMessage, probe.getRef());
    Response resp = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != resp);
  }

  @Test
  public void testGetBadgeIssuer() throws IOException {
    Response response = new Response();

    response.put(JsonKey.RESPONSE, createHttpUtilRes(200, mapToJson(createBadgeIssuerResponse())));

    PowerMockito.when(mockBadgingService.getIssuerDetails(actorMessage)).thenReturn(response);
    actorMessage.setOperation(BadgeOperations.getBadgeIssuer.name());
    subject.tell(actorMessage, probe.getRef());
    Response resp = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != resp);
  }

  @Test
  public void testGetListBadgeIssuer() throws IOException {
    Response response = new Response();

    List<Map<String, Object>> issuersList = new ArrayList<>();
    issuersList.add(createBadgeIssuerResponse());

    response.put(JsonKey.RESPONSE, createHttpUtilRes(200, listOfMapToJson(issuersList)));
    PowerMockito.when(mockBadgingService.getIssuerList(actorMessage)).thenReturn(response);
    actorMessage.setOperation(BadgeOperations.getAllIssuer.name());
    subject.tell(actorMessage, probe.getRef());
    Response resp = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != resp);
  }

  @Test
  public void testDeleteBadgeIssuer() throws IOException {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, createHttpUtilRes(200, "Issuer slug_01 deleted successfully ."));

    PowerMockito.when(mockBadgingService.deleteIssuer(actorMessage)).thenReturn(response);
    actorMessage.setOperation(BadgeOperations.deleteIssuer.name());
    subject.tell(actorMessage, probe.getRef());
    Response resp = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != resp);
  }

  private static String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";
    try {
      jsonResp = mapperObj.writeValueAsString(map);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return jsonResp;
  }

  private static String listOfMapToJson(List list) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";
    try {
      jsonResp = mapperObj.writeValueAsString(list);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return jsonResp;
  }

  private static Map<String, Object> createBadgeIssuerResponse() {
    Map<String, Object> bodyMap = new HashMap<>();
    bodyMap.put(BadgingJsonKey.SLUG, "slug_01");
    return bodyMap;
  }

  private static HttpUtilResponse createHttpUtilRes(int statusCode, String body) {

    HttpUtilResponse httpUtilResponse = new HttpUtilResponse();
    httpUtilResponse.setStatusCode(statusCode);
    httpUtilResponse.setBody(body);
    return httpUtilResponse;
  }
}
