package org.sunbird.learner.actors.recommend;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.cacheloader.PageCacheLoaderService;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.PageManagementActor;
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  DataCacheHandler.class,
  PageManagementActor.class,
  ContentSearchUtil.class,
  PageCacheLoaderService.class
})
@PowerMockIgnore({"javax.management.*"})
public class RecommendorActorTest {

  private static final ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(RecommendorActor.class);
  private static CassandraOperationImpl cassandraOperation;

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  @Before
  public void beforeTests() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordById());
  }

  private Response cassandraGetRecordById() {
    Response response = new Response();
    List list = new ArrayList();
    Map<String, Object> map = new HashMap<>();

    List<String> subList = new ArrayList<>();
    subList.add("english");
    subList.add("hindi");

    List<String> langList = new ArrayList<>();
    langList.add("english");
    langList.add("hindi");

    List<String> gradeList = new ArrayList<>();
    langList.add("A");
    langList.add("B");

    map.put(JsonKey.SUBJECT, subList);
    map.put(JsonKey.LANGUAGE, langList);
    map.put(JsonKey.GRADE, gradeList);
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Ignore
  @Test
  public void getRecommendedContentsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setRequestId("1");
    reqObj.setOperation(ActorOperations.GET_RECOMMENDED_COURSES.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, "USR");
    reqObj.setRequest(innerMap);

    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void getRecommendedContentsFailure() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setRequestId("1");
    reqObj.setOperation(ActorOperations.GET_RECOMMENDED_COURSES.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, "USR");
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordByIdFailure());

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidUserCredentials.getErrorCode()));
  }

  private Response cassandraGetRecordByIdFailure() {

    Response response = new Response();
    List list = new ArrayList();
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Test
  public void onReceiveTestWithInvalidOperation() throws Throwable {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setRequestId("1211");
    reqObj.setOperation("INVALID_OPERATION");
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc = probe.expectMsgClass(ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }
}
